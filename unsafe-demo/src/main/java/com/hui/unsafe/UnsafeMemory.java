package com.hui.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.charset.Charset;

public class UnsafeMemory
{
    private static final Unsafe UNSAFE;

    private static final long BYTE_ARRAY_BASE_OFFSET;

    private static final long BYTE_ARRAY_INDEX_SCALE;

    static
    {
        try
        {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
            BYTE_ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
            BYTE_ARRAY_INDEX_SCALE = UNSAFE.arrayIndexScale(byte[].class);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static final int EXP = 24;

    private static final long BLOCK_MEM_SIZE = 1 << EXP;// 2^24 bits, max memory size of block = 2^24 byte

    private long baseAddress;

    private long capicity;

    private int blockSize;// block size

    private long[] blockAddress;

    public UnsafeMemory(long capicity)
    {
        blockSize = (int) (capicity >> EXP) + 1;
        blockAddress = new long[blockSize];
        for (int i = 0; i < blockSize; i++)
        {
            blockAddress[i] = UNSAFE.allocateMemory(BLOCK_MEM_SIZE);
            UNSAFE.setMemory(blockAddress[i], BLOCK_MEM_SIZE, (byte) 0);
        }

        baseAddress = blockAddress[0];
        this.capicity = capicity;
    }

    public void free()
    {
        for (int i = 0; i < blockAddress.length; i++)
        {
            UNSAFE.freeMemory(blockAddress[i]);
        }

        blockSize = 0;
        blockAddress = null;
        capicity = 0;
        baseAddress = 0;
    }

    private void reallocate(long newCapicity)
    {
        int newBlockSize = (int) (newCapicity >> EXP) + 1;
        long[] newBlockAddress = new long[newBlockSize];
        for (int i = 0; i < blockSize; i++)
        {
            System.arraycopy(blockAddress, 0, newBlockAddress, 0, blockSize);
        }

        for (int i = blockSize; i < newBlockSize; i++)
        {
            newBlockAddress[i] = UNSAFE.allocateMemory(BLOCK_MEM_SIZE);
            UNSAFE.setMemory(newBlockAddress[i], BLOCK_MEM_SIZE, (byte) 0);
        }

        blockAddress = newBlockAddress;
        blockSize = newBlockSize;
        capicity = newCapicity;
    }


    public long getBaseAddress()
    {
        return baseAddress;
    }

    public void setBaseAddress(long baseAddress)
    {
        this.baseAddress = baseAddress;
    }

    public int getBlockSize()
    {
        return blockSize;
    }

    public void setBlockSize(int blockSize)
    {
        this.blockSize = blockSize;
    }

    public long[] getBlockAddress()
    {
        return blockAddress;
    }

    public void setBlockAddress(long[] blockAddress)
    {
        this.blockAddress = blockAddress;
    }

    public long getCapicity()
    {
        return capicity;
    }

    public void setCapicity(long capicity)
    {
        this.capicity = capicity;
    }

    /**
     * get real address from blocks by relative address
     * @param address relative address
     * @return real address
     */
    private long getRealAddress(long address)
    {
        if (address < baseAddress)
        {
            throw new IllegalArgumentException("illegal address");
        }

        if ((address - baseAddress) > (BLOCK_MEM_SIZE * blockSize))
        {
            throw new IllegalArgumentException("illegal address");
        }

        int block = (int) ((address - baseAddress) >> EXP);
        long blockPos = (address - baseAddress) & (BLOCK_MEM_SIZE - 1);
        return blockAddress[block] + blockPos;
    }

    /**
     * get real address from blocks by relative address and check if need to reallocate
     * @param address
     * @param typeSize
     * @return
     */
    private long getAndCheckRealAddress(long address, long typeSize)
    {
        if (address < baseAddress)
        {
            throw new IllegalArgumentException("illegal address");
        }

        // TODO:扩容limit
        while ((address - baseAddress) > (BLOCK_MEM_SIZE * blockSize - typeSize))
        {
            // scale out
            reallocate(capicity * 2);
        }

        int block = (int) ((address - baseAddress) >> EXP);
        long blockPos = (address - baseAddress) & (BLOCK_MEM_SIZE - 1);
        return blockAddress[block] + blockPos;
    }

    public byte getByte(long address)
    {
        return UNSAFE.getByte(getRealAddress(address));
    }

    public void putByte(long address, byte b)
    {
        UNSAFE.putByte(getAndCheckRealAddress(address, 1), b);
    }

    public short getShort(long address)
    {
        return UNSAFE.getShort(getRealAddress(address));
    }

    public void putShort(long address, short s)
    {
        UNSAFE.putShort(getAndCheckRealAddress(address, 2), s);
    }

    public char getChar(long address)
    {
        return UNSAFE.getChar(getRealAddress(address));
    }

    public void putChar(long address, char c)
    {
        UNSAFE.putChar(getAndCheckRealAddress(address, 2), c);
    }

    public int getInt(long address)
    {
        return UNSAFE.getInt(getRealAddress(address));
    }

    public void putInt(long address, int i)
    {
        UNSAFE.putInt(getAndCheckRealAddress(address, 4), i);
    }

    public long getLong(long address)
    {
        return UNSAFE.getLong(getRealAddress(address));
    }

    public void putLong(long address, long l)
    {
        UNSAFE.putLong(getAndCheckRealAddress(address, 8), l);
    }

    public float getFloat(long address)
    {
        return UNSAFE.getFloat(getRealAddress(address));
    }

    public void putFloat(long address, float f)
    {
        UNSAFE.putFloat(getAndCheckRealAddress(address, 4), f);
    }

    public double getDouble(long address)
    {
        return UNSAFE.getDouble(getRealAddress(address));
    }

    public void putDouble(long address, double d)
    {
        UNSAFE.putDouble(getAndCheckRealAddress(address, 8), d);
    }

    public byte[] getBytes(long address, int length)
    {
        byte[] bytes = new byte[length];
        long size = length * BYTE_ARRAY_INDEX_SCALE;

        long endPos = address + length * BYTE_ARRAY_INDEX_SCALE;

        if (address < baseAddress || (endPos - baseAddress) > (BLOCK_MEM_SIZE * blockSize))
        {
            throw new IllegalArgumentException("illegal address");
        }

        int beginBlock = (int) ((address - baseAddress) >> EXP);

        long beginBlockPos = (address - baseAddress) & (BLOCK_MEM_SIZE - 1);
        long beginReadAddress = blockAddress[beginBlock] + beginBlockPos;

        int endBlock = (int) ((endPos - baseAddress) >> EXP);
        long endBlockPos = (endPos - baseAddress) & (BLOCK_MEM_SIZE - 1);

        // not crossing blocks
        if (beginBlock == endBlock)
        {
            UNSAFE.copyMemory(null, beginReadAddress, bytes, BYTE_ARRAY_BASE_OFFSET, size);
            return bytes;
        }

        // crossing blocks
        long sz = BLOCK_MEM_SIZE - beginBlockPos;
        UNSAFE.copyMemory(null, beginReadAddress, bytes, BYTE_ARRAY_BASE_OFFSET, sz);
        for (int i = beginBlock + 1; i < endBlock; i++)
        {
            UNSAFE.copyMemory(null, blockAddress[i], bytes, BYTE_ARRAY_BASE_OFFSET + sz, BLOCK_MEM_SIZE);
            sz += BLOCK_MEM_SIZE;
        }
        if (endBlockPos > 0)
        {
            UNSAFE.copyMemory(null, blockAddress[endBlock], bytes, BYTE_ARRAY_BASE_OFFSET + sz, endBlockPos);
        }

        return bytes;
    }

    public void putBytes(long address, byte[] bytes)
    {
        long size = bytes.length * BYTE_ARRAY_INDEX_SCALE;

        long endPos = address + size;

        if (address < baseAddress)
        {
            throw new IllegalArgumentException("illegal address");
        }
        // make sure memory is enough
        getAndCheckRealAddress(address, size);

        int beginBlock = (int) ((address - baseAddress) >> EXP);
        long beginBlockPos = (address - baseAddress) & (BLOCK_MEM_SIZE - 1);
        long beginReadAddress = blockAddress[beginBlock] + beginBlockPos;

        int endBlock = (int) ((endPos - baseAddress) >> EXP);
        long endBlockPos = (endPos - baseAddress) & (BLOCK_MEM_SIZE - 1);

        // not crossing blocks
        if (beginBlock == endBlock)
        {
            UNSAFE.copyMemory(bytes, BYTE_ARRAY_BASE_OFFSET, null, beginReadAddress, size);
            return;
        }

        // crossing blocks
        long sz = BLOCK_MEM_SIZE - beginBlockPos;
        UNSAFE.copyMemory(bytes, BYTE_ARRAY_BASE_OFFSET, null, beginReadAddress, sz);
        for (int i = beginBlock + 1; i < endBlock; i++)
        {
            UNSAFE.copyMemory(bytes, BYTE_ARRAY_BASE_OFFSET + sz, null, blockAddress[i], BLOCK_MEM_SIZE);
            sz += BLOCK_MEM_SIZE;
        }
        if (endBlockPos > 0)
        {
            UNSAFE.copyMemory(bytes, BYTE_ARRAY_BASE_OFFSET + sz, null, blockAddress[endBlock], endBlockPos);
        }
    }
}
