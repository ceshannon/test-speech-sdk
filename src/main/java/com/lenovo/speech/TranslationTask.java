package com.lenovo.speech;

public interface TranslationTask extends Runnable{

	void enqueue(byte[] data);

	void destroy();

}
