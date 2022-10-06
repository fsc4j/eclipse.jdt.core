package fsc4j;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.IdentityHashMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
	}
	
	public void put(K key, V value) {
		this.map.put(new Key(key), value);
	}
	
	public V putIfAbsent(K key, V value) {
		return this.map.putIfAbsent(new Key(key), value);
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
	
}

interface RelatedObjectIterator<T> {
	void forEachRelatedObject(T subject, Consumer<Object> consumer);
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

public class EffectChecker {
	
	public static class SpecificationFrame {
		
		private final SpecificationFrame enclosingFrame;
		private final WeakConcurrentIdentityHashMap<Object, WeakHashMap<Class<?>, PermissionLevel>> permissions = new WeakConcurrentIdentityHashMap<>();

		private SpecificationFrame(SpecificationFrame enclosingFrame) {
			this.enclosingFrame = enclosingFrame;
		}
		
		private PermissionLevel getPermissionLevelFor(Class<?> classObject, Object object) {
			WeakHashMap<Class<?>, PermissionLevel> map = this.permissions.get(object);
			if (map == null)
				return PermissionLevel.NONE;
			return map.getOrDefault(classObject, PermissionLevel.NONE);
		}
		
		private void addPermissionFor(Class<?> classObject, Object object, PermissionLevel permissionLevel) {
			WeakHashMap<Class<?>, PermissionLevel> map = this.permissions.get(object);
			if (map == null) {
				map = new WeakHashMap<>();
				this.permissions.put(object, map);
			}
			map.put(classObject, PermissionLevel.max(map.getOrDefault(classObject, PermissionLevel.NONE), permissionLevel));
		}
		
		void assertCanCreate0(Object object, Class<?> classObject) {
			WeakConcurrentIdentityHashMap<Class<?>, UnitType> map0 = new WeakConcurrentIdentityHashMap<>(); 
			WeakConcurrentIdentityHashMap<Class<?>, UnitType> map = createdObjectsSet.putIfAbsent(object, map0);
			if (map == null)
				map = map0;
			UnitType value = map.putIfAbsent(classObject, UnitType.UNIT_VALUE);
			if (value == null)
				addPermissionFor(classObject, object, PermissionLevel.MUTATE);
		}
		
		void assertCanCreate(Object object, Class<?> classObject) {
			if (classObject == Object.class)
				return;
			assertCanCreate0(object, classObject);
			assertCanCreate(object, classObject.getSuperclass());
		}
		
		public void assertCanCreate(Object object) {
			assertCanCreate(object, object.getClass());
		}
		
		void assertPermission(Object object, Class<?> classObject, PermissionLevel level) {
			PermissionLevel permissionLevel = getPermissionLevelFor(classObject, object);
			if (permissionLevel.isLessThan(level))
				// TODO: Track the type of specification frame (method, constructor, assertion, ...) and produce a more specific error message
				throw new AssertionError(
						level == PermissionLevel.INSPECT ?
							"This code does not have permission to inspect this object."
						:
							"This code does not have permission to mutate this object.");
		}
		
		public void assertCanInspect(Object object, Class<?> classObject) {
			assertPermission(object, classObject, PermissionLevel.INSPECT);
		}
		
		public void assertCanInspect(Object object) {
			assertCanInspect(object, object.getClass());
		}
		
		public void assertCanMutate(Object object, Class<?> classObject) {
			assertPermission(object, classObject, PermissionLevel.MUTATE);
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
				if (classInfo != null) {
					classInfo.classLevelRepresentationObjectIterator.forEachRelatedObject(object, repObject -> {
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
					classInfo.classLevelRepresentationObjectIterator.forEachRelatedObject(object, repObject -> {
						requiresPermissionPublic(repObject, repObject.getClass(), level);
					});
					classInfo.packageLevelRepresentationObjectIterator.forEachRelatedObject(object, repObject -> {
						requiresPermissionPublic(repObject, repObject.getClass(), level);
					});
				}
			}
		}
		
		private void requiresPeerGroupPermissionInternal(Object object, Class<?> classObject, PermissionLevel level, IdentityHashMap<Object, UnitType> peerObjectsVisited) {
			requiresPermission1Internal(object, classObject, level);
			peerObjectsVisited.put(object, UnitType.UNIT_VALUE);
			ClassInfo classInfo = getClassInfo(classObject);
			if (classInfo != null)
				classInfo.classLevelPeerObjectIterator.forEachRelatedObject(object, peerObject -> {
					if (!peerObjectsVisited.containsKey(peerObject))
						requiresPeerGroupPermissionInternal(peerObject, peerObject.getClass(), level, peerObjectsVisited);
				});
		}
		
		private void requiresPeerGroupPermissionPublic(Object object, Class<?> classObject, PermissionLevel level, IdentityHashMap<Object, UnitType> peerObjectsVisited) {
			requiresPermission1Public(object, classObject, level);
			peerObjectsVisited.put(object, UnitType.UNIT_VALUE);
			ClassInfo classInfo = getClassInfo(classObject);
			if (classInfo != null) {
				classInfo.classLevelPeerObjectIterator.forEachRelatedObject(object, peerObject -> {
					if (!peerObjectsVisited.containsKey(peerObject))
						requiresPeerGroupPermissionPublic(peerObject, peerObject.getClass(), level, peerObjectsVisited);
				});
				classInfo.packageLevelPeerObjectIterator.forEachRelatedObject(object, peerObject -> {
					if (!peerObjectsVisited.containsKey(peerObject))
						requiresPeerGroupPermissionPublic(peerObject, peerObject.getClass(), level, peerObjectsVisited);
				});
			}
		}
		
		void requiresPermissionInternal(Object object, Class<?> classObject, PermissionLevel level) {
			requiresPeerGroupPermissionInternal(object, classObject, level, new IdentityHashMap<>());
		}
		
		void requiresPermissionPublic(Object object, Class<?> classObject, PermissionLevel level) {
			requiresPeerGroupPermissionPublic(object, classObject, level, new IdentityHashMap<>());
		}
		
		public boolean inspectsPrivate(Object object) {
			requiresPermission0(object, object.getClass(), PermissionLevel.INSPECT);
			return true;
		}
		
		public boolean inspectsAllPrivate(Object[] objects) {
			for (Object object : objects)
				inspectsPrivate(object);
			return true;
		}
		
		public boolean inspectsAllPrivate(Iterable<Object> objects) {
			for (Object object : objects)
				inspectsPrivate(object);
			return true;
		}
		
		public boolean mutatesPrivate(Object object) {
			requiresPermission0(object, object.getClass(), PermissionLevel.MUTATE);
			return true;
		}
		
		public boolean mutatesAllPrivate(Object[] objects) {
			for (Object object : objects)
				mutatesPrivate(object);
			return true;
		}
		
		public boolean mutatesAllPrivate(Iterable<Object> objects) {
			for (Object object : objects)
				mutatesPrivate(object);
			return true;
		}
		
		public boolean inspectsInternal(Object object) {
			requiresPermissionInternal(object, object.getClass(), PermissionLevel.INSPECT);
			return true;
		}
		
		public boolean inspectsAllInternal(Object[] objects) {
			for (Object object : objects)
				inspectsInternal(object);
			return true;
		}
		
		public boolean inspectsAllInternal(Iterable<Object> objects) {
			for (Object object : objects)
				inspectsInternal(object);
			return true;
		}
		
		public boolean mutatesInternal(Object object) {
			requiresPermissionInternal(object, object.getClass(), PermissionLevel.MUTATE);
			return true;
		}
		
		public boolean mutatesAllInternal(Object[] objects) {
			for (Object object : objects)
				mutatesInternal(object);
			return true;
		}
		
		public boolean mutatesAllInternal(Iterable<Object> objects) {
			for (Object object : objects)
				mutatesInternal(object);
			return true;
		}
		
		public boolean inspectsPublic(Object object) {
			requiresPermissionPublic(object, object.getClass(), PermissionLevel.INSPECT);
			return true;
		}
		
		public boolean inspectsAllPublic(Object[] objects) {
			for (Object object : objects)
				inspectsPublic(object);
			return true;
		}
		
		public boolean inspectsAllPublic(Iterable<Object> objects) {
			for (Object object : objects)
				inspectsPublic(object);
			return true;
		}
		
		public boolean mutatesPublic(Object object) {
			requiresPermissionPublic(object, object.getClass(), PermissionLevel.MUTATE);
			return true;
		}
		
		public boolean mutatesAllPublic(Object[] objects) {
			for (Object object : objects)
				mutatesPublic(object);
			return true;
		}
		
		public boolean mutatesAllPublic(Iterable<Object> objects) {
			for (Object object : objects)
				mutatesPublic(object);
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
	private static final WeakConcurrentIdentityHashMap<Object, WeakConcurrentIdentityHashMap<Class<?>, UnitType>> createdObjectsSet = new WeakConcurrentIdentityHashMap<>();
	private static final ThreadLocal<SpecificationFrame> specificationStackVariable = new ThreadLocal<>();
	
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
	
	public static void assertCanCreate(Object object, Class<?> classObject) {
		SpecificationFrame specificationStack = specificationStackVariable.get();
		if (specificationStack == null)
			return;
		specificationStack.assertCanCreate(object, classObject);
	}
	
	public static void assertCanCreate(Object object) {
		assertCanCreate(object, object.getClass());
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
