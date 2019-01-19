package com.hui.unsafe;

public class OffHeapMemory implements IMemory
{
    private long address;

    private UnsafeMemory memory;

    public OffHeapMemory(long capicity)
    {
        this.memory = new UnsafeMemory(capicity);
        this.address = memory.getBaseAddress();
    }

    @Override
    public byte getByte(long offset)
    {
        return memory.getByte(address + offset);
    }

    @Override
    public void putByte(long offset, byte b)
    {
        memory.putByte(address + offset, b);
    }

    @Override
    public short getShort(long offset)
    {
        return memory.getShort(address + offset);
    }

    @Override
    public void putShort(long offset, short s)
    {
        memory.putShort(address + offset, s);
    }

    @Override
    public char getChar(long offset)
    {
        return memory.getChar(address + offset);
    }

    @Override
    public void putChar(long offset, char c)
    {
        memory.putChar(address + offset, c);
    }

    @Override
    public int getInt(long offset)
    {
        return memory.getInt(address + offset);
    }

    @Override
    public void putInt(long offset, int i)
    {
        memory.putInt(address + offset, i);
    }

    @Override
    public long getLong(long offset)
    {
        return memory.getLong(address + offset);
    }

    @Override
    public void putLong(long offset, long l)
    {
        memory.putLong(address + offset, l);
    }

    @Override
    public float getFloat(long offset)
    {
        return memory.getFloat(address + offset);
    }

    @Override
    public void putFloat(long offset, float f)
    {
        memory.putFloat(address + offset, f);
    }

    @Override
    public double getDouble(long offset)
    {
        return memory.getDouble(address + offset);
    }

    @Override
    public void putDouble(long offset, double d)
    {
        memory.putDouble(address + offset, d);
    }

    @Override
    public byte[] getBytes(long offset, int length)
    {
        return memory.getBytes(address + offset, length);
    }

    @Override
    public void putBytes(long offset, byte[] bytes)
    {
        memory.putBytes(address + offset, bytes);
    }

    @Override
    public void free()
    {
        memory.free();
    }
}
