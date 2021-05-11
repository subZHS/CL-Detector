package de.tu_darmstadt.stg.mubench;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Main<E> {

    public void main(String[] args){

    }

    public class A {
        private int[] arr;

        public void method1(int para){
            arr = new int[para];
        }

        public void method2(int para){
            arr[0] = para;
        }
    }

    public class B {
        private A a = new A();

        public void method3(int para){
            a.method1(para);
        }

        public void method4(int para){
            a.method2(para);
        }
    }
}