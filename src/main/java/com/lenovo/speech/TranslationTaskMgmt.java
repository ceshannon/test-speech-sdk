package com.lenovo.speech;

import java.util.HashMap;
import java.util.Map;

public class TranslationTaskMgmt {
	
	private Map<String, TranslationTask> tasks = new HashMap<String, TranslationTask>();

	private static TranslationTaskMgmt mgmt = null;

	private TranslationTaskMgmt() {
	}

	public static TranslationTaskMgmt getInstance() {
		if (mgmt == null) {
			mgmt = new TranslationTaskMgmt();
		}
		return mgmt;
	}

	public static void register(String cid, TranslationTask TranslationTask) {
		getInstance().register0(cid, TranslationTask);
	}

	private void register0(String cid, TranslationTask TranslationTask) {
		tasks.put(cid, TranslationTask);
	}

	public static void unregister(String cid) {
		getInstance().unregister0(cid);
	}

	private void unregister0(String cid) {
		TranslationTask t = tasks.get(cid);
		if(t != null) {
			t.destroy();
		}
		tasks.remove(cid);
	}

	public static TranslationTask find(String cid) {
		return getInstance().find0(cid);
	}

	private TranslationTask find0(String cid) {
		return tasks.get(cid);
	}

}
