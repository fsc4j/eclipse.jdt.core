package fsc4j;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.IdentityHashMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Logger;

enum UnitType { UNIT_VALUE; }

class WeakConcurrentIdentityHashMap<K, V> {
	
	private static ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
	private static Thread cleanerThread = new Thread() {
		@Override
		public void run() {
			try {
				for (;;) {
					WeakConcurrentIdentityHashMap<?, ?>.Key key = (WeakConcurrentIdentityHashMap<?, ?>.Key)referenceQueue.remove();
					key.remove();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};
	static {
		cleanerThread.setDaemon(true);
		cleanerThread.start();
	}
	
	private ConcurrentHashMap<Key, V> map = new ConcurrentHashMap<Key, V>();
	
	private class Key extends WeakReference<K> {
		final int hashCode;
		Key(K object) {
			super(object, referenceQueue);
			this.hashCode = System.identityHashCode(object);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (!(obj instanceof WeakConcurrentIdentityHashMap<?, ?>.Key))
				return false;
			WeakConcurrentIdentityHashMap<?, ?>.Key other = (WeakConcurrentIdentityHashMap<?, ?>.Key)obj;
			Object thisReferent = this.get();
			if (thisReferent == null)
				return false;
			Object otherReferent = other.get();
			return thisReferent == otherReferent;
		}
		
		@Override
		public int hashCode() {
			return this.hashCode;
		}
		
		void remove() {
			WeakConcurrentIdentityHashMap.this.map.remove(this);
		}
		
		@Override
		public String toString() {
			return Integer.toHexString(this.hashCode);
		}
	}
	
	public void put(K key, V value) {
		this.map.put(new Key(key), value);
	}
	
	public V putIfAbsent(K key, V value) {
		return this.map.putIfAbsent(new Key(key), value);
	}
	
	public boolean replace(K key, V oldValue, V newValue) {
		return this.map.replace(new Key(key), oldValue, newValue);
	}

	public V get(K key) {
		return this.map.get(new Key(key));
	}
	
	public V getOrDefault(K key, V defaultValue) {
		return this.map.getOrDefault(new Key(key), defaultValue);
	}
	
	public void forEachEntry(BiConsumer<K, V> consumer) {
		for (Key key : this.map.keySet()) {
			K referent = key.get();
			if (referent != null)
				consumer.accept(referent, this.map.get(key));
		}
	}
	
	@Override
	public String toString() {
		return map.toString();
	}
	
}

class ClassInfo {
	final RelatedObjectIterator<Object> classLevelRepresentationObjectIterator;
	final RelatedObjectIterator<Object> classLevelPeerObjectIterator;
	final RelatedObjectIterator<Object> packageLevelRepresentationObjectIterator;
	final RelatedObjectIterator<Object> packageLevelPeerObjectIterator;
	
	ClassInfo(
			RelatedObjectIterator<Object> classLevelRepresentationObjectIterator,
			RelatedObjectIterator<Object> classLevelPeerObjectIterator,
			RelatedObjectIterator<Object> packageLevelRepresentationObjectIterator,
			RelatedObjectIterator<Object> packageLevelPeerObjectIterator
	) {
		this.classLevelRepresentationObjectIterator = classLevelRepresentationObjectIterator;
		this.classLevelPeerObjectIterator = classLevelPeerObjectIterator;
		this.packageLevelRepresentationObjectIterator = packageLevelRepresentationObjectIterator;
		this.packageLevelPeerObjectIterator = packageLevelPeerObjectIterator;
	}
}

enum PermissionLevel {
	
	NONE(0),
	INSPECT(1),
	MUTATE(2);
	
	final int level;
	
	PermissionLevel(int level) { this.level = level; }
	
	boolean isLessThan(PermissionLevel other) {
		return this.level < other.level;
	}
	
	static PermissionLevel max(PermissionLevel level1, PermissionLevel level2) {
		return level1.isLessThan(level2) ? level2 : level1;
	}
	
}

enum PermissionToken {
	CLAIMED_INSPECT,
	DELEGATED_INSPECT,
	IMMUTABLE,
	MUTATE;
}

public class EffectChecker {
	
	private static final Logger logger = Logger.getLogger("fsc4j.EffectChecker");
	
	public static class SpecificationFrame {
		
		private final SpecificationFrame enclosingFrame;
		private final WeakConcurrentIdentityHashMap<Object, WeakHashMap<Class<?>, PermissionToken>> permissions = new WeakConcurrentIdentityHashMap<>();

		private SpecificationFrame(SpecificationFrame enclosingFrame) {
			this.enclosingFrame = enclosingFrame;
		}
		
		private WeakHashMap<Class<?>, PermissionToken> getPermissionMapForObject(Object object) {
			WeakHashMap<Class<?>, PermissionToken> map = this.permissions.get(object);
			if (map == null) {
				map = new WeakHashMap<>();
				this.permissions.put(object, map);
			}
			return map;
		}
		
		private void addPermissionFor(Class<?> classObject, Object object, PermissionToken token) {
			WeakHashMap<Class<?>, PermissionToken> map = getPermissionMapForObject(object);
			PermissionToken oldToken = map.get(classObject);
			if (oldToken == null || token == PermissionToken.MUTATE) {
				map.put(classObject, token);
			} // Otherwise, we have a PermissionLevel.INSPECT token and we are adding a PermissionLevel.INSPECT token so do nothing.
		}

		private void addPermissionFor(Class<?> classObject, Object object, PermissionLevel level) {
			addPermissionFor(classObject, object, level == PermissionLevel.MUTATE ? PermissionToken.MUTATE : PermissionToken.DELEGATED_INSPECT);
		}

		void assertPermission(Object object, Class<?> classObject, PermissionLevel level) {
			switch (level) {
			case INSPECT: assertCanInspect(object, classObject); break;
			case MUTATE: assertCanMutate(object, classObject); break;
			default: throw new AssertionError("Internal FSC4J error");
			}
		}
		
		public void assertCanInspect(Object object, Class<?> classObject) {
			logger.fine(() -> String.format("assertCanInspect(%x, %s) with permissions=%s and claimedPermissions=%s", System.identityHashCode(object), classObject, permissions, claimedPermissions));
			if (classObject == Object.class)
				return;
			WeakHashMap<Class<?>, PermissionToken> permissionMap = getPermissionMapForObject(object);
			PermissionToken token = permissionMap.get(classObject);
			if (token != null)
				return;
			
			// Try to claim permission
			token = PermissionToken.CLAIMED_INSPECT;
			var claimedPermissionsMap = getClaimedPermissionsMapForObject(object);
			PermissionToken oldClaimedToken = claimedPermissionsMap.putIfAbsent(classObject, token);
			if (oldClaimedToken == null) {
				permissionMap.put(classObject, token);
				return;
			}
			if (oldClaimedToken == PermissionToken.IMMUTABLE) {
				permissionMap.put(classObject, oldClaimedToken);
				return;
			}
			if (oldClaimedToken == PermissionToken.CLAIMED_INSPECT) {
				// Someone else has claimed an INSPECT permission; try to turn it into an IMMUTABLE permission
				token = PermissionToken.IMMUTABLE;
				if (claimedPermissionsMap.replace(classObject, oldClaimedToken, token)) {
					permissionMap.put(classObject, token);
					return;
				}
				// If the inspect token is gone, that means it was replaced either by a MutateToken or an ImmutableToken.
				oldClaimedToken = claimedPermissionsMap.get(classObject);
				if (oldClaimedToken == PermissionToken.IMMUTABLE) {
					permissionMap.put(classObject, oldClaimedToken);
					return;
				}
			}
			// If we get here, someone (not us) has claimed a MutableToken on this object frame.
			throw new AssertionError("This code does not have permission to inspect this object.");
		}
		
		public void assertCanInspect(Object object) {
			assertCanInspect(object, object.getClass());
		}
		
		public void assertCanMutate(Object object, Class<?> classObject) {
			logger.fine(() -> String.format("assertCanMutate(%x, %s) with permissions=%s and claimedPermissions=%s", System.identityHashCode(object), classObject, permissions, claimedPermissions));
			if (classObject == Object.class)
				return;
			WeakHashMap<Class<?>, PermissionToken> permissionMap = getPermissionMapForObject(object);
			PermissionToken token = permissionMap.get(classObject);
			if (token == PermissionToken.MUTATE)
				return;
			if (token == PermissionToken.CLAIMED_INSPECT) {
				var claimedPermissionsMap = getClaimedPermissionsMapForObject(object);
				if (claimedPermissionsMap.replace(classObject, token, PermissionToken.MUTATE)) {
					permissionMap.put(classObject, PermissionToken.MUTATE);
					return;
				}
				throw new AssertionError("This code does not have permission to mutate this object; it is immutable.");
			} else if (token == PermissionToken.IMMUTABLE) {
				throw new AssertionError("This code does not have permission to mutate this object; it is immutable.");
			} else {
				if (token == null) {
					var claimedPermissionsMap = getClaimedPermissionsMapForObject(object);
					if (claimedPermissionsMap.putIfAbsent(classObject, PermissionToken.MUTATE) == null) {
						permissionMap.put(classObject, PermissionToken.MUTATE);
						return;
					}
				}
				throw new AssertionError("This code does not have permission to mutate this object.");
			}
		}
		
		public void assertCanMutate(Object object) {
			assertCanMutate(object, object.getClass());
		}
		
		private void requiresPermission0(Object object, Class<?> classObject, PermissionLevel level) {
			if (this.enclosingFrame != null)
				this.enclosingFrame.assertPermission(object, classObject, level);
			addPermissionFor(classObject, object, level);
		}
		
		private void requiresPermission1Internal(Object object, Class<?> classObject, PermissionLevel level) {
			if (classObject != Object.class) {
				requiresPermission0(object, classObject, level);
				ClassInfo classInfo = getClassInfo(classObject);
				if (classInfo != null && classInfo.classLevelRepresentationObjectIterator != null) {
					classInfo.classLevelRepresentationObjectIterator.forEachRelatedObject(object, repObject -> {
						if (repObject != null)
							requiresPermissionPublic(repObject, repObject.getClass(), level);
					});
				}
			}
		}
		
		private void requiresPermission1Public(Object object, Class<?> classObject, PermissionLevel level) {
			if (classObject != Object.class) {
				requiresPermission0(object, classObject, level);
				requiresPermission1Public(object, classObject.getSuperclass(), level);
				ClassInfo classInfo = getClassInfo(classObject);
				if (classInfo != null) {
					if (classInfo.classLevelRepresentationObjectIterator != null) {
						classInfo.classLevelRepresentationObjectIterator.forEachRelatedObject(object, repObject -> {
							if (repObject != null)
								requiresPermissionPublic(repObject, repObject.getClass(), level);
						});
					}
					if (classInfo.packageLevelRepresentationObjectIterator != null) {
						classInfo.packageLevelRepresentationObjectIterator.forEachRelatedObject(object, repObject -> {
							if (repObject != null)
								requiresPermissionPublic(repObject, repObject.getClass(), level);
						});
					}
				}
			}
		}
		
		private void requiresPeerGroupPermissionInternal(Object object, Class<?> classObject, PermissionLevel level, IdentityHashMap<Object, UnitType> peerObjectsVisited) {
			requiresPermission1Internal(object, classObject, level);
			peerObjectsVisited.put(object, UnitType.UNIT_VALUE);
			ClassInfo classInfo = getClassInfo(classObject);
			if (classInfo != null && classInfo.classLevelPeerObjectIterator != null)
				classInfo.classLevelPeerObjectIterator.forEachRelatedObject(object, peerObject -> {
					if (peerObject != null && !peerObjectsVisited.containsKey(peerObject))
						requiresPeerGroupPermissionInternal(peerObject, peerObject.getClass(), level, peerObjectsVisited);
				});
		}
		
		private void requiresPeerGroupPermissionPublic(Object object, Class<?> classObject, PermissionLevel level, IdentityHashMap<Object, UnitType> peerObjectsVisited) {
			requiresPermission1Public(object, classObject, level);
			peerObjectsVisited.put(object, UnitType.UNIT_VALUE);
			ClassInfo classInfo = getClassInfo(classObject);
			if (classInfo != null) {
				if (classInfo.classLevelPeerObjectIterator != null) {
					classInfo.classLevelPeerObjectIterator.forEachRelatedObject(object, peerObject -> {
						if (peerObject != null && !peerObjectsVisited.containsKey(peerObject))
							requiresPeerGroupPermissionPublic(peerObject, peerObject.getClass(), level, peerObjectsVisited);
					});
				}
				if (classInfo.packageLevelPeerObjectIterator != null) {
					classInfo.packageLevelPeerObjectIterator.forEachRelatedObject(object, peerObject -> {
						if (peerObject != null && !peerObjectsVisited.containsKey(peerObject))
							requiresPeerGroupPermissionPublic(peerObject, peerObject.getClass(), level, peerObjectsVisited);
					});
				}
			}
		}
		
		private void requiresPermissionInternal(Object object, Class<?> classObject, PermissionLevel level) {
			requiresPeerGroupPermissionInternal(object, classObject, level, new IdentityHashMap<>());
		}
		
		private void requiresPermissionPublic(Object object, Class<?> classObject, PermissionLevel level) {
			requiresPeerGroupPermissionPublic(object, classObject, level, new IdentityHashMap<>());
		}
		
		public boolean inspectsPrivate(Object object) {
			if (object != null)
				requiresPermission0(object, object.getClass(), PermissionLevel.INSPECT);
			return true;
		}
		
		public boolean mutatesPrivate(Object object) {
			if (object != null)
				requiresPermission0(object, object.getClass(), PermissionLevel.MUTATE);
			return true;
		}
		
		public boolean inspectsInternal(Object object) {
			if (object != null)
				requiresPermissionInternal(object, object.getClass(), PermissionLevel.INSPECT);
			return true;
		}
		
		public boolean mutatesInternal(Object object) {
			if (object != null)
				requiresPermissionInternal(object, object.getClass(), PermissionLevel.MUTATE);
			return true;
		}
		
		public boolean inspectsPublic(Object object) {
			if (object != null)
				requiresPermissionPublic(object, object.getClass(), PermissionLevel.INSPECT);
			return true;
		}
		
		public boolean mutatesPublic(Object object) {
			if (object != null)
				requiresPermissionPublic(object, object.getClass(), PermissionLevel.MUTATE);
			return true;
		}
		
		public void pop() {
			if (specificationStackVariable.get() != this)
				throw new AssertionError("Cannot pop a specification frame that is not the top frame.");
			if (this.enclosingFrame != null) {
				this.permissions.forEachEntry((key, value) -> {
					for (Class<?> classObject : value.keySet())
						this.enclosingFrame.addPermissionFor(classObject, key, value.get(classObject));
				});
			}
			specificationStackVariable.set(this.enclosingFrame);
		}
		
	}
	
	private static final WeakHashMap<Class<?>, WeakReference<ClassInfo>> classInfoMap = new WeakHashMap<>();
	private static final WeakConcurrentIdentityHashMap<Object, WeakConcurrentIdentityHashMap<Class<?>, PermissionToken>> claimedPermissions = new WeakConcurrentIdentityHashMap<>();
	private static final ThreadLocal<SpecificationFrame> specificationStackVariable = new ThreadLocal<>();
	
	private static WeakConcurrentIdentityHashMap<Class<?>, PermissionToken> getClaimedPermissionsMapForObject(Object object) {
		WeakConcurrentIdentityHashMap<Class<?>, PermissionToken> map0 = new WeakConcurrentIdentityHashMap<>();
		WeakConcurrentIdentityHashMap<Class<?>, PermissionToken> map = claimedPermissions.putIfAbsent(object, map0);
		if (map == null)
			return map0;
		return map;
	}

	@SuppressWarnings("unchecked")
	public synchronized static <T> void registerClassInfo(
			Class<T> classObject,
			RelatedObjectIterator<T> classLevelRepresentationObjectIterator,
			RelatedObjectIterator<T> classLevelPeerObjectIterator,
			RelatedObjectIterator<T> packageLevelRepresentationObjectIterator,
			RelatedObjectIterator<T> packageLevelPeerObjectIterator
	) {
		if (classInfoMap.containsKey(classObject))
			throw new AssertionError("The class info for this class has already been registered.");
		ClassInfo classInfo = new ClassInfo(
				(RelatedObjectIterator<Object>)classLevelRepresentationObjectIterator,
				(RelatedObjectIterator<Object>)classLevelPeerObjectIterator,
				(RelatedObjectIterator<Object>)packageLevelRepresentationObjectIterator,
				(RelatedObjectIterator<Object>)packageLevelPeerObjectIterator
		);
		classInfoMap.put(classObject, new WeakReference<>(classInfo));
	}
	
	private synchronized static ClassInfo getClassInfo(Class<?> classObject) {
		WeakReference<ClassInfo> classInfoReference = classInfoMap.get(classObject);
		if (classInfoReference == null)
			return null;
		return classInfoReference.get();
	}
	
	public static void assertCanInspect(Object object, Class<?> classObject) {
		SpecificationFrame specificationStack = specificationStackVariable.get();
		if (specificationStack == null)
			return;
		specificationStack.assertCanInspect(object, classObject);
	}
	
	public static void assertCanInspect(Object object) {
		SpecificationFrame specificationStack = specificationStackVariable.get();
		if (specificationStack == null)
			return;
		specificationStack.assertCanInspect(object);
	}
	
	public static void assertCanMutate(Object object, Class<?> classObject) {
		SpecificationFrame specificationStack = specificationStackVariable.get();
		if (specificationStack == null)
			return;
		specificationStack.assertCanMutate(object, classObject);
	}
	
	public static void assertCanMutate(Object object) {
		SpecificationFrame specificationStack = specificationStackVariable.get();
		if (specificationStack == null)
			return;
		specificationStack.assertCanMutate(object);
	}
	
	public static SpecificationFrame pushNewFrame() {
		SpecificationFrame specificationStack = specificationStackVariable.get();
		SpecificationFrame result = new SpecificationFrame(specificationStack);
		specificationStackVariable.set(result);
		return result;
	}
	
}
