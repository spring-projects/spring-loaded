package enums

enum WhatAnEnum3 implements ExtensibleEnum3 {

    PETS_AT_THE_DISCO(1),
    JUMPING_INTO_A_HOOP(2),
    HAVING_A_NICE_TIME(3),
    LIVING_ON_A_LOG(4),
    WHAT_DID_YOU_DO(5),
    WOBBLE(6),
    UNKNOWN(0)

    final int value
	
    private WhatAnEnum3(int intValue) {
        this.value = intValue
    }

    private static final Map<Integer, WhatAnEnum3> MAP = [:] as Map<Integer, WhatAnEnum3>
	
    static {
        WhatAnEnum3.values().each { WhatAnEnum3 response ->
            MAP.put(response.value, response)
        }
    }
}
