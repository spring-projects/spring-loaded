package enums;

public class RunnerA {

	public static void main(String[] args) {
		run();
	}

	public static void run() {
		System.out.println(WhatAnEnum.RED);
		System.out.println(WhatAnEnum.GREEN);
		System.out.println(WhatAnEnum.BLUE);
		WhatAnEnum[] vals = WhatAnEnum.values();
		System.out.print("[");
		for (int i = 0; i < vals.length; i++) {
			if (i > 0) {
				System.out.print(" ");
			}
			System.out.print(vals[i]==null?"NULL":vals[i].toString());//+" "+vals[i].getIntValue());
		}
		System.out.println("]");
		System.out.println("value count = " + vals.length);
	}
}
