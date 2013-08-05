package com.bcel.test;

public class Test {

	public static void main(String[] args) {
		// excepciones
		// try {
		// exc();
		// throw new Throwable();
		// } catch (Throwable e) {
		// print(e.getMessage());
		// }

		// objetos simple
		// obj(a,5);
		// Arrays y objetos
		// A a = new A(5);
		// A[] array = new A[5];
		// array[0] = a;
		// A a = new A(20);
		// for (int i = 0; i < 2; i++) {
		// if (i == 1)
		// a.set(i);
		// }

		// statics
		// a.valStatic=a.get();
		// int b=a.valStatic;
		// // creacion objetos
		// String a = new String("cadena");

		// String str=A.strStatic;
		// System.out.println();
		// metodos nativos
		// print("------print nativo------");
		// println("------println nativo------");
		// int a = getIntFromStr("2");
		// foo(5);
		// if ( c> 0){
		// print("5 es mayor que 0");
		// }
		// else {
		// print("5 no es mayor que 0");
		// }
		// System.out.print(fibIter(5));

		// arrays
	}

	public int nostatic(int a, int b){
		return 5;
	}
	public static int intexp(int a, int n) throws Throwable {
		if (n < 0) // Exponent must be non-negative
			throw new Throwable();
		else if ((a == 0) && (n == 0)) // 0 to 0 is undefined
			throw new Throwable();
		else {
			int out = 1;
			for (; n > 0; n--)
				out = out * a;
			return out;
		}
	}

	public static boolean sorted(int[] xs) {
		boolean sorted = true;
		int length = xs.length;
		int i = 1;
		while ((i < length) && (sorted == true)) {
			if (xs[i] > xs[i - 1])
				i++;
			else
				sorted = false;
		}
		return sorted;
	}

	public static void bubbleSort(int a[]) {
		for (int i = a.length; --i >= 0;) {
			for (int j = 0; j < i; j++) {
				if (a[j] > a[j + 1]) {
					int T = a[j];
					a[j] = a[j + 1];
					a[j + 1] = T;
				}
			}
		}
	}

	public static int arrayObj(A[] array) {
		A[] a = new A[2];
		if (array[0] == null)
			return 0;
		else
			return 1;
	}

	public static int array(int[] array) {
		int[] a = new int[2];
		array[0] = array[1];
		if (array[0] == 5)
			return 0;
		else
			return 1;
	}

	public static void obj(A a, int c) {
		// //b es el field del objeto a
		// if (c >= 0) {
		// a.setStatic(c);
		// } else
		// a.getStatic();
		// b es el field del objeto a
		if (c >= 0) {
			a.set(c);
			a.set(5);
		} else
			a.get();

	}

	public static void exc() throws Throwable {
		throw new Throwable("desdeexc");
	}

	public static int abs(int x) {
		x = -x + 5;
		x = 5;
		if (x >= 0)
			return x;
		else
			return -x;
	}

	public static int fact(int x) {
		int out = 1;
		while (x > 0) {
			out = out * x;
			x--;
		}
		return out;
	}

	public static int fact2(int n) {
		int out = 1;
		for (; n > 0; n--)
			out = out * n;
		return out;
	}

	public static int factRec(int n) {
		if (n == 0) {
			return 1;
		} else
			return n * factRec(n - 1);
	}

	public static int multiFact(int n, int k) {
		if ((0 <= n) && (n < k))
			return 1;
		else
			return n * multiFact(n - k, k);
	}

	public static int quadFact(int n) {
		return fact(2 * n) / fact(n);
	}

	public static int fibIter(int n) {
		int aux;
		int act = 1;
		int pre = 1;
		while (n > 1) {
			aux = act;
			act = pre + act;
			pre = aux;
			n--;
		}
		return act;
	}

	public static int fibRec(int n) {
		if (n == 0)
			return 1;
		else if (n == 1)
			return 1;
		else
			return fibRec(n - 1) + fibRec(n - 2);
	}

	public static int exp(int base, int exponent) {
		int result = 1;
		int i = exponent;
		while (i > 0) {
			result *= base;
			i--;
		}
		return result;
	}

	public static int foo(int n, int m) {
		m = m + 5;
		return m;
	}

	public static Rational[] simp(Rational[] rs) {
		int length = rs.length;
		Rational[] oldRs = new Rational[length];
		Rational.arraycopy(rs, oldRs, length);
		for (int i = 0; i < length; i++)
			rs[i].simplify();
		return oldRs;
	}

	public static native void print(String str);

	public static native void println(String str);

	public static native int getIntFromStr(String str);
}
