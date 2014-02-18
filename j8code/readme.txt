The main springloaded codebase is not yet using Java8. The Java8 'stuff' is captured here in this
project and included in the distributed agent. It should only be loaded if we find ourselves on a
Java8 VM. This code is not using Java8 source constructs, it is calling Java8 APIs.