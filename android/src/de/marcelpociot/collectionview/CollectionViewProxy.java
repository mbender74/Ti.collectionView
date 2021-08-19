package de.marcelpociot.collectionview;


import java.util.ArrayList;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

@Kroll.proxy(creatableInModule = AndroidcollectionviewModule.class, propertyAccessors = {
	TiC.PROPERTY_HEADER_TITLE,
	TiC.PROPERTY_FOOTER_TITLE,
	TiC.PROPERTY_DEFAULT_ITEM_TEMPLATE,
	TiC.PROPERTY_SHOW_VERTICAL_SCROLL_INDICATOR,
	TiC.PROPERTY_SEPARATOR_COLOR,
	TiC.PROPERTY_SEARCH_TEXT,
	TiC.PROPERTY_SEARCH_VIEW,
	TiC.PROPERTY_CASE_INSENSITIVE_SEARCH,
	TiC.PROPERTY_HEADER_DIVIDERS_ENABLED,
	TiC.PROPERTY_FOOTER_DIVIDERS_ENABLED
})
public class CollectionViewProxy extends TiViewProxy {

	private static final String TAG = "CollectionViewProxy";
	
	private static final int MSG_FIRST_ID = TiViewProxy.MSG_LAST_ID + 1;

	private static final int MSG_SECTION_COUNT = MSG_FIRST_ID + 399;
	private static final int MSG_SCROLL_TO_ITEM = MSG_FIRST_ID + 400;
	private static final int MSG_APPEND_SECTION = MSG_FIRST_ID + 401;
	private static final int MSG_INSERT_SECTION_AT = MSG_FIRST_ID + 402;
	private static final int MSG_DELETE_SECTION_AT = MSG_FIRST_ID + 403;
	private static final int MSG_REPLACE_SECTION_AT = MSG_FIRST_ID + 404;
	private static final int MSG_GET_SECTIONS = MSG_FIRST_ID + 405;
	private static final int MSG_SET_SECTIONS = MSG_FIRST_ID + 406;



	//indicate if user attempts to add/modify/delete sections before TiListView is created 
	private boolean preload = false;
	private ArrayList<CollectionSectionProxy> preloadSections;
	private HashMap<String, Integer> preloadMarker;
	
	public CollectionViewProxy() {
		super();
		defaultValues.put("columnWidth", 0);
		defaultValues.put("verticalSpacing", 0);
		defaultValues.put("horizontalSpacing", 0);
	}

	public TiUIView createView(Activity activity) {
		return new CollectionView(this, activity);
	}
	
	public void handleCreationArgs(KrollModule createdInModule, Object[] args) {
		preloadSections = new ArrayList<CollectionSectionProxy>();
		defaultValues.put(TiC.PROPERTY_DEFAULT_ITEM_TEMPLATE, "listDefaultTemplate");
		defaultValues.put(TiC.PROPERTY_CASE_INSENSITIVE_SEARCH, true);
		super.handleCreationArgs(createdInModule, args);
		
	}
	public void handleCreationDict(KrollDict options) {
		super.handleCreationDict(options);
		//Adding sections to preload sections, so we can handle appendSections/insertSection
		//accordingly if user call these before TiListView is instantiated.
		if (options.containsKey(TiC.PROPERTY_SECTIONS)) {
			Object obj = options.get(TiC.PROPERTY_SECTIONS);
			if (obj instanceof Object[]) {
				addPreloadSections((Object[]) obj, -1, true);
			}
		}
	}
	
	public void clearPreloadSections() {
		if (preloadSections != null) {
			preloadSections.clear();
		}
	}
	
	public ArrayList<CollectionSectionProxy> getPreloadSections() {
		return preloadSections;
	}
	
	public boolean getPreload() {
		return preload;
	}
	
	public void setPreload(boolean pload)
	{
		preload = pload;
	}
	
	public HashMap<String, Integer> getPreloadMarker()
	{
		return preloadMarker;
	}

	private void addPreloadSections(Object secs, int index, boolean arrayOnly) {
		if (secs instanceof Object[]) {
			Object[] sections = (Object[]) secs;
			for (int i = 0; i < sections.length; i++) {
				Object section = sections[i];
				addPreloadSection(section, -1);
			}
		} else if (!arrayOnly) {
			addPreloadSection(secs, -1);
		}
	}
	
	private void addPreloadSection(Object section, int index) {
		if (section instanceof CollectionSectionProxy) {
			if (index == -1) {
				preloadSections.add((CollectionSectionProxy) section);
			} else {
				preloadSections.add(index, (CollectionSectionProxy) section);
			}
		}
	}
	
	@Kroll.method @Kroll.getProperty
	public int getSectionCount() {
		if (TiApplication.isUIThread()) {
			return handleSectionCount();
		} else {
			return (Integer) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SECTION_COUNT));
		}
	}
	
	public int handleSectionCount () {
		TiUIView listView = peekView();
		if (listView != null) {
			return ((CollectionView) listView).getSectionCount();
		}
		return 0;
	}

	@Kroll.method
	public void scrollToItem(int sectionIndex, int itemIndex, @SuppressWarnings("rawtypes") @Kroll.argument(optional=true)HashMap options) {
		boolean animated = true;
		if ( (options != null) && (options instanceof HashMap<?, ?>) ) {
			@SuppressWarnings("unchecked")
			KrollDict animationargs = new KrollDict(options);
			if (animationargs.containsKeyAndNotNull(TiC.PROPERTY_ANIMATED)) {
				animated = TiConvert.toBoolean(animationargs.get(TiC.PROPERTY_ANIMATED), true);
			}
		} 
		if (TiApplication.isUIThread()) {
			handleScrollToItem(sectionIndex, itemIndex, animated);
		} else {
			KrollDict d = new KrollDict();
			d.put("itemIndex", itemIndex);
			d.put("sectionIndex", sectionIndex);
			d.put(TiC.PROPERTY_ANIMATED, Boolean.valueOf(animated));
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SCROLL_TO_ITEM), d);
		}
	}
	
	@Kroll.method
	public void setMarker(Object marker) {
		if (marker instanceof HashMap) {
			HashMap<String, Integer> m = (HashMap<String, Integer>) marker;
			TiUIView listView = peekView();
			if (listView != null) {
				((CollectionView)listView).setMarker(m);
			} else {
				preloadMarker = m;
			}
		}
	}
	

	@Override
	public boolean handleMessage(final Message msg) 	{

		switch (msg.what) {

			case MSG_SECTION_COUNT: {
				AsyncResult result = (AsyncResult)msg.obj;
				result.setResult(handleSectionCount());
				return true;
			}

			case MSG_SCROLL_TO_ITEM: {
				AsyncResult result = (AsyncResult)msg.obj;
				KrollDict data = (KrollDict) result.getArg();
				int sectionIndex = data.getInt("sectionIndex");
				int itemIndex = data.getInt("itemIndex");
				boolean animated = data.getBoolean(TiC.PROPERTY_ANIMATED);
				handleScrollToItem(sectionIndex, itemIndex, animated);
				result.setResult(null);
				return true;
			}
			case MSG_APPEND_SECTION: {
				AsyncResult result = (AsyncResult)msg.obj;
				handleAppendSection(result.getArg());
				result.setResult(null);
				return true;
			}
			case MSG_DELETE_SECTION_AT: {
				AsyncResult result = (AsyncResult)msg.obj;
				handleDeleteSectionAt(TiConvert.toInt(result.getArg()));
				result.setResult(null);
				return true;
			}
			case MSG_INSERT_SECTION_AT: {
				AsyncResult result = (AsyncResult)msg.obj;
				KrollDict data = (KrollDict) result.getArg();
				int index = data.getInt("index");
				Object section = data.get("section");
				handleInsertSectionAt(index, section);
				result.setResult(null);
				return true;
			}
			case MSG_REPLACE_SECTION_AT: {
				AsyncResult result = (AsyncResult)msg.obj;
				KrollDict data = (KrollDict) result.getArg();
				int index = data.getInt("index");
				Object section = data.get("section");
				handleReplaceSectionAt(index, section);
				result.setResult(null);
				return true;
			}
			
			case MSG_GET_SECTIONS: {
				AsyncResult result = (AsyncResult)msg.obj;
				result.setResult(handleSections());
				return true;
			}
			
			case MSG_SET_SECTIONS: {
				AsyncResult result = (AsyncResult)msg.obj;
				TiUIView listView = peekView();
				if (listView != null) {
					((CollectionView)listView).processSectionsAndNotify((Object[])result.getArg());
				} else {
					Log.e(TAG, "Unable to set sections, listView is null", Log.DEBUG_MODE);
				}
				result.setResult(null);
				return true;
			}
			
			default:
				return super.handleMessage(msg);
		}
	}
	private void handleScrollToItem(int sectionIndex, int itemIndex, boolean animated) {
		TiUIView listView = peekView();
		if (listView != null) {
			((CollectionView) listView).scrollToItem(sectionIndex, itemIndex, animated);
		}
	}
	
	@Kroll.method
	public void appendSection(Object section) {
		if (TiApplication.isUIThread()) {
			handleAppendSection(section);
		} else {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_APPEND_SECTION), section);
		}
	}

	private void handleAppendSection(Object section) {
		TiUIView listView = peekView();
		if (listView != null) {
			((CollectionView) listView).appendSection(section);
		} else {
			preload = true;
			addPreloadSections(section, -1, false);
		}
	}
	
	@Kroll.method
	public void deleteSectionAt(int index) {
		if (TiApplication.isUIThread()) {
			handleDeleteSectionAt(index);
		} else {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_DELETE_SECTION_AT), index);
		}
	}
	
	private void handleDeleteSectionAt(int index) {
		TiUIView listView = peekView();
		if (listView != null) {
			((CollectionView) listView).deleteSectionAt(index);
		} else {
			if (index < 0 || index >= preloadSections.size()) {
				Log.e(TAG, "Invalid index to delete section");
				return;
			}
			preload = true;
			preloadSections.remove(index);
		}
	}
	
	@Kroll.method
	public void insertSectionAt(int index, Object section) {
		if (TiApplication.isUIThread()) {
			handleInsertSectionAt(index, section);
		} else {
			sendInsertSectionMessage(index, section);
		}
	}
	
	private void sendInsertSectionMessage(int index, Object section) {
		KrollDict data = new KrollDict();
		data.put("index", index);
		data.put("section", section);
		TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_INSERT_SECTION_AT), data);
	}
	
	private void handleInsertSectionAt(int index, Object section) {
		TiUIView listView = peekView();
		if (listView != null) {
			((CollectionView) listView).insertSectionAt(index, section);
		} else {
			if (index < 0 || index > preloadSections.size()) {
				Log.e(TAG, "Invalid index to insertSection");
				return;
			}
			preload = true;
			addPreloadSections(section, index, false);
		}
	}
	
	@Kroll.method
	public void replaceSectionAt(int index, Object section) {
		if (TiApplication.isUIThread()) {
			handleReplaceSectionAt(index, section);
		} else {
			sendReplaceSectionMessage(index, section);
		}
	}
	
	private void sendReplaceSectionMessage(int index, Object section) {
		KrollDict data = new KrollDict();
		data.put("index", index);
		data.put("section", section);
		TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_REPLACE_SECTION_AT), data);
	}

	private void handleReplaceSectionAt(int index, Object section) {
		TiUIView listView = peekView();
		if (listView != null) {
			((CollectionView) listView).replaceSectionAt(index, section);
		} else {
			handleDeleteSectionAt(index);
			handleInsertSectionAt(index,  section);
			
		}
	}
	
	@Kroll.method @Kroll.getProperty
	public CollectionSectionProxy[] getSections()
	{
		if (TiApplication.isUIThread()) {
			return handleSections();
		} else {
			return (CollectionSectionProxy[]) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GET_SECTIONS));
		}
	}
	
	@Kroll.setProperty @Kroll.method
	public void setSections(Object sections)
	{
		if (!(sections instanceof Object[])) {
			Log.e(TAG, "Invalid argument type to setSection(), needs to be an array", Log.DEBUG_MODE);
			return;
		}
		//Update java and javascript property
		setProperty(TiC.PROPERTY_SECTIONS, sections);

		Object[] sectionsArray = (Object[]) sections;
		TiUIView listView = peekView();
		//Preload sections if listView is not opened.
		if (listView == null) {
			preload = true;
			clearPreloadSections();
			addPreloadSections(sectionsArray, -1, true);
		} else {
			if (TiApplication.isUIThread()) {
				((CollectionView)listView).processSectionsAndNotify(sectionsArray);
			} else {
				TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_SECTIONS), sectionsArray);
			}
			
		}
	}
	
	private CollectionSectionProxy[] handleSections()
	{
		TiUIView listView = peekView();

		if (listView != null) {
			return ((CollectionView) listView).getSections();
		}
		ArrayList<CollectionSectionProxy> preloadedSections = getPreloadSections();
		return preloadedSections.toArray(new CollectionSectionProxy[preloadedSections.size()]);
	}

	@Kroll.method
	public void setRefreshing(boolean refreshing) {
		TiUIView listView = peekView();
		
		if (listView != null) {
			((CollectionView) listView).setRefreshing(refreshing);
		}	
	}
	
	@Kroll.method @Kroll.getProperty
	public boolean isRefreshing() {
		TiUIView listView = peekView();
		
		if (listView != null) {
			return ((CollectionView) listView).isRefreshing();
		}
		
		return false;
	}

	
	@Override
	public String getApiName()
	{
		return "ti.collectionview";
	}
}
