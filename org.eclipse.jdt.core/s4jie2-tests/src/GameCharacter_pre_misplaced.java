// Check that FSC4J tags in the wrong place do not crash the compiler

/**
 * @representationObject
 * @representationObjects
 * @peerObject
 * @peerObjects
 * @pre | false
 * @representationObject
 * @representationObjects
 * @peerObject
 * @peerObjects
 */
class MisplacedPrePost {
	/** @pre | false */
	int x;
}

/** @pre | false */
interface MisplacedPrePostInterface {
	
}

/** @pre | false */
@interface MisplacedPrePostAnnotation {
	
}

/** @pre | false */
enum MisplacedPrePostEnum {
	/** @pre | false */
	MISPLACED_PRE_POST_ENUM_CONSTANT;
}
