package enums;

public class RunnerB {

	public static void main(String[] args) {
		run();
	}

	public static void run() {
		System.out.println(WhatAnEnumB.HAVING_A_NICE_TIME);
		System.out.println(WhatAnEnumB.JUMPING_INTO_A_HOOP);
		System.out.println(WhatAnEnumB.LIVING_ON_A_LOG);
		WhatAnEnumB[] vals = WhatAnEnumB.values();
		System.out.print("[");
		for (int i = 0; i < vals.length; i++) {
			if (i > 0) {
				System.out.print(" ");
			}
			System.out.print(vals[i]+" "+vals[i].getIntValue());
		}
		System.out.println("]");
		System.out.println("value count = " + vals.length);
	}
}
