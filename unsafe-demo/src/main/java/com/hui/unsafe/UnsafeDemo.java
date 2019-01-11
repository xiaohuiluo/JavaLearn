package com.hui.unsafe;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

public class UnsafeDemo {

    private static Unsafe UNSEFE = null;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSEFE = (Unsafe) field.get(null);
        } catch (Exception e) {
            /* ... */
        }
    }

    public static void main(String[] args) {
        final Thread mainThread = Thread.currentThread();
        System.out.println("Start main Thread ...");
        new Thread(() ->
        {
            try {
                Thread.sleep(3000);
                System.out.println("child thread ...");
                UNSEFE.unpark(mainThread);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        UNSEFE.park(false, 0);
        System.out.println("End main Thread ...");
    }
}
