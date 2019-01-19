package com.hui.unsafe;


import java.nio.charset.Charset;

public class UnsafeTest
{
    private static final byte[] BYTES = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-=!@#$%^&*()".getBytes();

    private static final int REPETITIONS = 1000;

    private static final int BLOCK = 1024*32;

    private static final byte[][] BYTES_ARRAY = new byte[BLOCK][REPETITIONS];

    static
    {
        for (int i = 0; i < REPETITIONS; i++)
        {
            for (int j = 0; j < BLOCK; j++)
            {
                BYTES_ARRAY[j][i] = BYTES[j%BYTES.length];
            }
        }
    }

    private static void testBytes()
    {
        try
        {
            IMemory memory = new OffHeapMemory(100);

            String hello = "123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            byte[] bytes = hello.getBytes(Charset.forName("UTF-8"));
            memory.putBytes(0, bytes);
            byte[] result = memory.getBytes(3, bytes.length-3);
            String helleResult = new String(result, "UTF-8");
            System.out.println(helleResult);

            memory.free();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void testInt()
    {
        try
        {
            IMemory memory = new OffHeapMemory(100);

            memory.putInt(0, 100);
            System.out.println(memory.getInt(0));
            memory.putInt(0, 10);
            System.out.println(memory.getInt(0));
            memory.putInt(4, 1000);
            memory.putInt(8, 100000);
            System.out.println(memory.getInt(4));
            memory.putInt(12, 10000);

            memory.putInt(1024*64*2 + 1000 + 4, 2000);
            System.out.println(memory.getInt(1024*64*2 + 1000 + 4));

            System.out.println(memory.getInt(12));
            System.out.println(memory.getInt(8));

            memory.putInt(1024*64*2 + 1000 + 8, 3000);
            System.out.println(memory.getInt(1024*64*2 + 1000 + 4));
            System.out.println(memory.getInt(1024*64*2 + 1000 + 8));

            memory.free();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
       testInt();
       testBytes();
    }

}
