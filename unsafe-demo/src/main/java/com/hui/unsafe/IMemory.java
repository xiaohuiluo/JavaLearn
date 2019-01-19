package com.hui.unsafe;

public interface IMemory
{
    byte getByte(long offset);
    void putByte(long offset, byte b);

    short getShort(long offset);
    void putShort(long offset, short s);

    char getChar(long offset);
    void putChar(long offset, char c);

    int getInt(long offset);
    void putInt(long offset, int i);

    long getLong(long offset);
    void putLong(long offset, long l);

    float getFloat(long offset);
    void putFloat(long offset, float f);

    double getDouble(long offset);
    void putDouble(long offset, double d);

    byte[] getBytes(long offset, int length);
    void putBytes(long offset, byte[] bytes);

    void free();
}
