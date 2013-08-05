package com.bcel.test;

public class Rational{

    private int num;
    private int den;

    public Rational(){
        num = 0;
        den = 1;
    }

    public Rational(int n,int d){
        num = n;
        den = d;
    }

    public static int abs(int x){
        if (x >= 0) return x;
        else return -x;
    }

    public static int gcd(int a,int b){
        int res;
        while (b != 0){
            res = a%b;
            a = b;
            b = res;
        }
        return abs(a);
    }
    
    public static void arraycopy(Object[] src,Object[] dest,int length){
        if (length < 0) throw new ArithmeticException();//ArrayIndexOutOfBoundsException();                                                                                                                       
        for (int i = 0; i < length; i++)
            dest[i] = src[i];
    }
    
    public void simplify(){
        int gcd = gcd(num,den);
        num = num/gcd;
        den = den/gcd;
    }
    
    public boolean equals(Object obj){
        if (obj instanceof Rational){
                Rational rat= (Rational) obj;
                return this.num==rat.num && this.den==rat.den;
             }
        return false;
    }

}
