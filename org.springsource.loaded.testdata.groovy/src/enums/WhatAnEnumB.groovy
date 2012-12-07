package enums

enum WhatAnEnumB implements ExtensibleEnumB {

    PETS_AT_THE_DISCO(1),
    JUMPING_INTO_A_HOOP(2),
    HAVING_A_NICE_TIME(3),
    LIVING_ON_A_LOG(4),
    WHAT_DID_YOU_DO(5),
    UNKNOWN(0)

    final int intValue
    private WhatAnEnumB(int intValue) {
        this.intValue = intValue
    }

    private static final Map<Integer, WhatAnEnumB> MAP = [:] as Map<Integer, WhatAnEnumB>
    static {
        WhatAnEnumB.values().each { WhatAnEnumB response ->
            MAP.put(response.intValue, response)
        }
    }
}
