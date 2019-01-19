package com.hui.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeUtil
{

    private static final Unsafe UNSAFE;

    private static final long BYTE_ARRAY_BASE_OFFSET;

    static
    {
        try
        {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
            BYTE_ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void putByte(long address, byte value)
    {
        UNSAFE.putByte(address, value);
    }

    public static byte getByte(long address)
    {
        return UNSAFE.getByte(address);
    }

    public static byte getByte(byte[] data, int index)
    {
        return UNSAFE.getByte(data, BYTE_ARRAY_BASE_OFFSET + index);
    }

    public static void putByte(byte[] data, int index, byte value)
    {
        UNSAFE.putByte(data, BYTE_ARRAY_BASE_OFFSET + index, value);
    }

    /**
     * unsafe park unpark
     */
    private static void testParkUnpark()
    {
        final Thread mainThread = Thread.currentThread();
        System.out.println("Start main Thread ...");
        new Thread(() ->
        {
            try
            {
                Thread.sleep(3000);
                System.out.println("child thread ...");
                UNSAFE.unpark(mainThread);// unpark main thread
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }).start();

        UNSAFE.park(false, 0);// park main thread
        System.out.println("End main Thread ...");
    }

    public static void main(String[] args)
    {
       testParkUnpark();
    }
}
