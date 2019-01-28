package com.hui.unsafe;

import sun.misc.Unsafe;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;

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

    static class AtomicCounter
    {
        private volatile long counter;

        private long offset;

        public AtomicCounter()
        {
            try
            {
                offset = UNSAFE.objectFieldOffset(AtomicCounter.class.getDeclaredField("counter"));
            }
            catch (NoSuchFieldException e)
            {
                // kill exception
            }
        }

        public void increment()
        {
            long expect = counter;
            while (!UNSAFE.compareAndSwapLong(this, offset, expect, expect + 1))
            {
                expect = counter;
            }
        }

        public long getCounter()
        {
            return counter;
        }
    }

    public static void testAtomicCounter()
    {
        AtomicCounter counter = new AtomicCounter();

        CountDownLatch latch = new CountDownLatch(1000);

        ThreadFactory threadFactory = new ThreadFactory()
        {
            @Override
            public Thread newThread(Runnable r)
            {
                return new Thread(r);
            }
        };

        for (int i = 0; i < 1000; i++)
        {
            threadFactory.newThread(() ->
            {
                counter.increment();
                latch.countDown();
            }).start();
        }

        try
        {
            latch.await();
        }
        catch (InterruptedException e)
        {
            //kill
        }

        System.out.println(counter.getCounter());
    }

    static class Cloner
    {
        private int id;

        private int age;

        public Cloner()
        {
        }

        public Cloner(int id, int age)
        {
            this.id = id;
            this.age = age;
        }

        @Override
        public String toString()
        {
            return "Cloner{" +
                    "id=" + id +
                    ", age=" + age +
                    '}';
        }

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        public int getAge()
        {
            return age;
        }

        public void setAge(int age)
        {
            this.age = age;
        }
    }


    public static long sizeOf(Object object)
    {
        Class clazz = object.getClass();
        long maximumOffset = 0;
        Class maxiNumFieldClass = null;
        do
        {
            for (Field f : clazz.getDeclaredFields())
            {
                if (!Modifier.isStatic(f.getModifiers()))
                {
                    long tmp = UNSAFE.objectFieldOffset(f);
                    Class fieldClass = f.getType();
                    if (tmp > maximumOffset)
                    {
                        maximumOffset = tmp;
                        maxiNumFieldClass = fieldClass;
                    }
                }
            }
        }
        while ((clazz = clazz.getSuperclass()) != null);

        long last = getClassOffset(maxiNumFieldClass);
        return addPaddingSize(maximumOffset + last);
    }

    private static long addPaddingSize(long size)
    {
        if (size % 8 != 0)
        {
            return (size / 8 + 1) * 8;
        }
        return size;
    }

    private static long getClassOffset(Class fieldClass)
    {
        return (long) (byte.class.equals(fieldClass) ? 1 :
                (short.class.equals(fieldClass) || char.class.equals(fieldClass)) ? 2 :
                        (long.class.equals(fieldClass) || double.class.equals(fieldClass)) ? 8 : 4);
    }

    private static long addressOf(Object object)
    {
        Object[] array = new Object[]{object};
        long baseOffset = UNSAFE.arrayBaseOffset(Object[].class);
        int addressSize = UNSAFE.addressSize();
        long location;
        switch (addressSize)
        {
            case 4:
                location = UNSAFE.getInt(array, baseOffset);
                break;
            case 8:
                location = UNSAFE.getLong(array, baseOffset);
                break;
            default:
                throw new Error("unsupported address size: " + addressSize);
        }

        return (location);
    }

    private static Cloner fromAddress(long addr)
    {
        Cloner[] array = new Cloner[]{null};
        long baseOffset = UNSAFE.arrayBaseOffset(Cloner[].class);
        UNSAFE.putLong(array, baseOffset, addr);
        UNSAFE.freeMemory(addr);
        return array[0];
    }


//    public static Cloner shallowCopy(Object object)
//    {
//        long size = sizeOf(object);
//        long srcAddress = addressOf(object);
////        long address = UNSAFE.allocateMemory(size);
////        UNSAFE.copyMemory(start, address, size);
//
//        Cloner cloner = new Cloner();
//        long tarAddress = addressOf(cloner);
//        UNSAFE.copyMemory(srcAddress, tarAddress, size);
//        return cloner;
//        return fromAddress(address);
//    }

    public static Cloner deepCopy(Object object)
    {
        long size = sizeOf(object);
        long srcAddress = addressOf(object);

        Cloner cloner = new Cloner();
        long tarAddress = addressOf(cloner);
        UNSAFE.copyMemory(srcAddress, tarAddress, size);
        return cloner;
    }

    public static void testShallowCopy()
    {
        Cloner cloner = new Cloner(123, 20);
        System.out.println("src cloner: " + cloner);
        Cloner cloner1 = deepCopy(cloner);
        System.out.println("dest cloner: " +cloner1);
        cloner1.setId(234);
        cloner1.setAge(30);
        System.out.println("after set src cloner: " +cloner);
        System.out.println("after set dest cloner: " +cloner1);

//        System.out.println(cloner1.getId() + ":" + cloner1.getAge());
    }

    public static void main(String[] args)
    {
        testShallowCopy();
        testAtomicCounter();
        testParkUnpark();
    }
}
