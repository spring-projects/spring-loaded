package enums

enum WhatAnEnumB2 implements ExtensibleEnumB {
	
    PETS_AT_THE_DISCO(1),
    JUMPING_INTO_A_HOOP(2),
    HAVING_A_NICE_TIME(3),
    LIVING_ON_A_LOG(4),
    WHAT_DID_YOU_DO(5),
    WOBBLE(6),
    UNKNOWN(0)

    final int intValue
	
    private WhatAnEnumB2(int intValue) {
        this.intValue = intValue
    }

    private static final Map<Integer, WhatAnEnumB2> MAP = [:] as Map<Integer, WhatAnEnumB2>
	
    static {
        WhatAnEnumB2.values().each { WhatAnEnumB2 response ->
            MAP.put(response.intValue, response)
        }
    }
}
