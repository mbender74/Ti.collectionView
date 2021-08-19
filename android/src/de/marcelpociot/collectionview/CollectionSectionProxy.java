package de.marcelpociot.collectionview;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;

import de.marcelpociot.collectionview.CollectionView.TiBaseAdapter;
import de.marcelpociot.collectionview.CollectionViewTemplate.DataItem;
import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.ViewProxy;
import ti.modules.titanium.ui.widget.TiView;
import android.app.Activity;
import android.os.Message;
import android.view.View;

@Kroll.proxy(creatableInModule = AndroidcollectionviewModule.class, propertyAccessors = {
})
public class CollectionSectionProxy extends TiViewProxy{

	private static final String TAG = "CollectionSectionProxy";
	private ArrayList<CollectionItemData> listItemData;
	private int itemCount;
	private TiBaseAdapter adapter;
	private ArrayList<Object> itemProperties;
	private ArrayList<Integer> filterIndices;
	private boolean preload;
	
	private WeakReference<CollectionView> listView;
	public DefaultCollectionViewTemplate builtInTemplate;
	
	private String headerTitle;
	private String footerTitle;
	
	private TiViewProxy headerView;
	private TiViewProxy footerView;

	
	private static final int MSG_FIRST_ID = TiViewProxy.MSG_LAST_ID + 1;

	private static final int MSG_SET_ITEMS = MSG_FIRST_ID + 700;
	private static final int MSG_APPEND_ITEMS = MSG_FIRST_ID + 701;
	private static final int MSG_INSERT_ITEMS_AT = MSG_FIRST_ID + 702;
	private static final int MSG_DELETE_ITEMS_AT = MSG_FIRST_ID + 703;
	private static final int MSG_GET_ITEM_AT = MSG_FIRST_ID + 704;
	private static final int MSG_REPLACE_ITEMS_AT = MSG_FIRST_ID + 705;
	private static final int MSG_UPDATE_ITEM_AT = MSG_FIRST_ID + 706;
	private static final int MSG_GET_ITEMS = MSG_FIRST_ID + 707;
	private static final int MSG_SET_HEADER_TITLE = MSG_FIRST_ID + 708;
	private static final int MSG_SET_FOOTER_TITLE = MSG_FIRST_ID + 709;
	private static final int MSG_SET_HEADER_VIEW = MSG_FIRST_ID + 710;
	private static final int MSG_SET_FOOTER_VIEW = MSG_FIRST_ID + 711;



	
	@Override
	public TiUIView createView(Activity activity)
	{
		TiUIView view = new TiView(this);
		view.getLayoutParams().autoFillsHeight = true;
		view.getLayoutParams().autoFillsWidth = true;
		return view;
	}
	
	
	public class CollectionItemData {
		private KrollDict properties;
		private CollectionViewTemplate template;
		private String searchableText = "";
		public CollectionItemData (KrollDict properties, CollectionViewTemplate template) {
			this.properties = properties;
			this.template = template;
			//set searchableText
			if (properties.containsKey(TiC.PROPERTY_PROPERTIES)) {
				Object props = properties.get(TiC.PROPERTY_PROPERTIES);
				if (props instanceof HashMap) {
					HashMap<String, Object> propsHash = (HashMap<String, Object>) props;
					Object searchText = propsHash.get(TiC.PROPERTY_SEARCHABLE_TEXT);
					if (propsHash.containsKey(TiC.PROPERTY_SEARCHABLE_TEXT) && searchText != null) {
						searchableText = TiConvert.toString(searchText);
					}
				}
			}

		}

		public KrollDict getProperties() {
			return properties;
		}
		
		public String getSearchableText() {
			return searchableText;
		}
		
		public CollectionViewTemplate getTemplate() {
			return template;
		}
	}
	
	public CollectionSectionProxy () {
		//initialize variables
		listItemData = new ArrayList<CollectionItemData>();
		filterIndices = new ArrayList<Integer>();
		itemCount = 0;
		preload = false;
	}
	
	public void handleCreationDict(KrollDict dict) {
		//getting header/footer titles from creation dictionary
		if (dict.containsKey(TiC.PROPERTY_HEADER_TITLE)) {
			headerTitle = TiConvert.toString(dict, TiC.PROPERTY_HEADER_TITLE);
		}
		if (dict.containsKey(TiC.PROPERTY_FOOTER_TITLE)) {
			footerTitle = TiConvert.toString(dict, TiC.PROPERTY_FOOTER_TITLE);
		}
		if (dict.containsKey(TiC.PROPERTY_HEADER_VIEW)) {
			Object obj = dict.get(TiC.PROPERTY_HEADER_VIEW);
			if (obj instanceof TiViewProxy) {
				headerView = (TiViewProxy) obj;
			}
		}
		if (dict.containsKey(TiC.PROPERTY_FOOTER_VIEW)) {
			Object obj = dict.get(TiC.PROPERTY_FOOTER_VIEW);
			if (obj instanceof TiViewProxy) {
				footerView = (TiViewProxy) obj;
			}
		}

		if (dict.containsKey(TiC.PROPERTY_ITEMS)) {
			handleSetItems(dict.get(TiC.PROPERTY_ITEMS));
		}
	}
	
	public void setAdapter(TiBaseAdapter a) {
		adapter = a;
	}

	@Kroll.method @Kroll.setProperty
	public void setHeaderView(TiViewProxy headerView) {
		if (TiApplication.isUIThread()) {
			handleSetHeaderView(headerView);
		} else {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_HEADER_VIEW), headerView);
		}
	}
	
	@Kroll.method @Kroll.getProperty
	public TiViewProxy getHeaderView() {
		return headerView;
	}
	
	@Kroll.method @Kroll.setProperty
	public void setFooterView(TiViewProxy footerView) {
		if (TiApplication.isUIThread()) {
			handleSetFooterView(footerView);
		} else {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_FOOTER_VIEW), footerView);
		}
	}
	
	@Kroll.method @Kroll.getProperty
	public TiViewProxy getFooterView() {
		return footerView;
	}

	@Kroll.method @Kroll.setProperty
	public void setHeaderTitle(String headerTitle) {
		if (TiApplication.isUIThread()) {
			handleSetHeaderTitle(headerTitle);
		} else {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_HEADER_TITLE), headerTitle);
		}
	}
	
	@Kroll.method @Kroll.getProperty
	public String getHeaderTitle() {
		return headerTitle;
	}
	
	@Kroll.method @Kroll.setProperty
	public void setFooterTitle(String footerTitle) {
		if (TiApplication.isUIThread()) {
			handleSetFooterTitle(footerTitle);
		} else {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_FOOTER_TITLE), footerTitle);
		}
	}
	
	@Kroll.method @Kroll.getProperty
	public String getFooterTitle() {
		return footerTitle;
	}
	
	public String getHeaderOrFooterTitle(int index) {
		if (isHeaderTitle(index)) {
			return headerTitle;
		} else if (isFooterTitle(index)) {
			return footerTitle;
		}
		return "";
	}

	public View getHeaderOrFooterView(int index) {
		if (isHeaderView(index)) {
			return getListView().layoutHeaderOrFooterView(headerView);
		} else if (isFooterView(index)) {
			return getListView().layoutHeaderOrFooterView(footerView);
		}
		return null;
	}



	@Override
	public boolean handleMessage(Message msg) 
	{
		switch (msg.what) {

			case MSG_SET_ITEMS: {
				AsyncResult result = (AsyncResult) msg.obj;
				handleSetItems(result.getArg());
				result.setResult(null);
				return true;
			}
			
			case MSG_SET_HEADER_TITLE: {
				AsyncResult result = (AsyncResult) msg.obj;
				handleSetHeaderTitle(TiConvert.toString(result.getArg()));
				result.setResult(null);
				return true;
			}

			case MSG_SET_FOOTER_TITLE: {
				AsyncResult result = (AsyncResult) msg.obj;
				handleSetFooterTitle(TiConvert.toString(result.getArg()));
				result.setResult(null);
				return true;
			}

			case MSG_SET_HEADER_VIEW: {
				AsyncResult result = (AsyncResult) msg.obj;
				handleSetHeaderView((TiViewProxy)result.getArg());
				result.setResult(null);
				return true;
			}

			case MSG_SET_FOOTER_VIEW: {
				AsyncResult result = (AsyncResult) msg.obj;
				handleSetFooterView((TiViewProxy)result.getArg());
				result.setResult(null);
				return true;
			}


			case MSG_GET_ITEMS: {
				AsyncResult result = (AsyncResult) msg.obj;
				result.setResult(itemProperties.toArray());
				return true;
			}

			case MSG_APPEND_ITEMS: {
				AsyncResult result = (AsyncResult) msg.obj;
				handleAppendItems(result.getArg());
				result.setResult(null);
				return true;
			}

			case MSG_INSERT_ITEMS_AT: {
				AsyncResult result = (AsyncResult) msg.obj;
				KrollDict data = (KrollDict) result.getArg();
				int index = data.getInt(TiC.EVENT_PROPERTY_INDEX);
				handleInsertItemsAt(index, data.get(TiC.PROPERTY_DATA));
				result.setResult(null);
				return true;
			}

			case MSG_DELETE_ITEMS_AT: {
				AsyncResult result = (AsyncResult) msg.obj;
				KrollDict data = (KrollDict) result.getArg();
				int index = data.getInt(TiC.EVENT_PROPERTY_INDEX);
				int count = data.getInt(TiC.PROPERTY_COUNT);
				handleDeleteItemsAt(index, count);
				result.setResult(null);
				return true;
			}

			case MSG_REPLACE_ITEMS_AT: {
				AsyncResult result = (AsyncResult) msg.obj;
				KrollDict data = (KrollDict) result.getArg();
				int index = data.getInt(TiC.EVENT_PROPERTY_INDEX);
				int count = data.getInt(TiC.PROPERTY_COUNT);
				handleReplaceItemsAt(index, count, data.get(TiC.PROPERTY_DATA));
				result.setResult(null);
				return true;
			}

			case MSG_GET_ITEM_AT: {
				AsyncResult result = (AsyncResult) msg.obj;
				KrollDict item = handleGetItemAt(TiConvert.toInt(result.getArg()));
				result.setResult(item);
				return true;
			}
			
			case MSG_UPDATE_ITEM_AT: {
				AsyncResult result = (AsyncResult) msg.obj;
				KrollDict data = (KrollDict) result.getArg();
				int index = data.getInt(TiC.EVENT_PROPERTY_INDEX);
				handleUpdateItemAt(index, data.get(TiC.PROPERTY_DATA));
				result.setResult(null);
				return true;
			}

			default : {
				return super.handleMessage(msg);
			}

		}
	}

	@Kroll.method
	public KrollDict getItemAt(int index) {
		if (TiApplication.isUIThread()) {
			return handleGetItemAt(index);
		} else {
			return (KrollDict) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GET_ITEM_AT), index);
		}
	}
	
	private KrollDict handleGetItemAt(int index) {
		if (itemProperties != null && index >= 0 && index < itemProperties.size()) {
			return new KrollDict((HashMap)itemProperties.get(index));
		}
		return null;
	}

	@Kroll.method @Kroll.setProperty
	public void setItems(Object data) {
		if (TiApplication.isUIThread()) {
			handleSetItems(data);
		} else {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_ITEMS), data);
		}
	}
	
	@Kroll.method @Kroll.getProperty
	public Object[] getItems() {
		if (itemProperties == null) {
			return new Object[0];
		} else if (TiApplication.isUIThread()) {
			return itemProperties.toArray();
		} else {
			return (Object[]) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GET_ITEMS));
		}
	}
	
	@Kroll.method
	public void appendItems(Object data) {
		if (TiApplication.isUIThread()) {
			handleAppendItems(data);
		} else {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_APPEND_ITEMS), data);
		}
	}
	
	public boolean isIndexValid(int index) {
		return (index >= 0) ? true : false;
	}
	
	@Kroll.method
	public void insertItemsAt(int index, Object data) {
		if (!isIndexValid(index)) {
			return;
		}
		
		if (TiApplication.isUIThread()) {
			handleInsertItemsAt(index, data);
		} else {
			KrollDict d = new KrollDict();
			d.put(TiC.PROPERTY_DATA, data);
			d.put(TiC.EVENT_PROPERTY_INDEX, index);
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_INSERT_ITEMS_AT), d);
		}
	}
	
	@Kroll.method
	public void deleteItemsAt(int index, int count) {
		if (!isIndexValid(index)) {
			return;
		}

		if (TiApplication.isUIThread()) {
			handleDeleteItemsAt(index, count);
		} else {
			KrollDict d = new KrollDict();
			d.put(TiC.EVENT_PROPERTY_INDEX, index);
			d.put(TiC.PROPERTY_COUNT, count);
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_DELETE_ITEMS_AT), d);
		}
	}
	
	@Kroll.method
	public void replaceItemsAt(int index, int count, Object data) {
		if (!isIndexValid(index)) {
			return;
		}

		if (TiApplication.isUIThread()) {
			handleReplaceItemsAt(index, count, data);
		} else {
			KrollDict d = new KrollDict();
			d.put(TiC.EVENT_PROPERTY_INDEX, index);
			d.put(TiC.PROPERTY_COUNT, count);
			d.put(TiC.PROPERTY_DATA, data);
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_REPLACE_ITEMS_AT), d);
		}
	}
	
	@Kroll.method
	public void updateItemAt(int index, Object data) {
		if (!isIndexValid(index) || !(data instanceof HashMap)) {
			return;
		}

		if (TiApplication.isUIThread()) {
			handleUpdateItemAt(index,  new Object[]{data});
		} else {
			KrollDict d = new KrollDict();
			d.put(TiC.EVENT_PROPERTY_INDEX, index);
			d.put(TiC.PROPERTY_DATA, new Object[]{data});
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_UPDATE_ITEM_AT), d);
		}
	}

	
	public void processPreloadData() {
		if (itemProperties != null && preload) {
			handleSetItems(itemProperties.toArray());
			preload = false;
		}
	}
	
	public void refreshItems() {
		handleSetItems(itemProperties.toArray());
	}

	private void processData(Object[] items, int offset) {
		if (listItemData == null) {
			return;
		}
		
		CollectionViewTemplate[] temps = new CollectionViewTemplate[items.length];
		//First pass through data, we process template and update
		//default properties based data given
		for (int i = 0; i < items.length; i++) {
			Object itemData = items[i];
			if (itemData instanceof HashMap) {
				KrollDict d = new KrollDict((HashMap)itemData);
				CollectionViewTemplate template = processTemplate(d, i + offset);
				template.updateOrMergeWithDefaultProperties(d, true);
				temps[i] = template;
			}
		}
		//Second pass we would merge properties
		for (int i = 0; i < items.length; i++) {
			Object itemData = items[i];
			if (itemData instanceof HashMap) {
				KrollDict d = new KrollDict((HashMap)itemData);
				CollectionViewTemplate template = temps[i];
				if (template != null) {
					template.updateOrMergeWithDefaultProperties(d, false);
				}
				CollectionItemData itemD = new CollectionItemData(d, template);
				d.remove(TiC.PROPERTY_TEMPLATE);
				listItemData.add(i+offset, itemD);
			}
		}
		//Notify adapter that data has changed.
		adapter.notifyDataSetChanged();
	}

	private void handleSetItems(Object data) {

		if (data instanceof Object[]) {
			Object[] items = (Object[]) data;
			itemProperties =  new ArrayList<Object>(Arrays.asList(items));
			listItemData.clear();
			//only process items when listview's properties is processed.
			if (getListView() == null) {
				preload = true;
				return;
			}
			itemCount = items.length;
			processData(items, 0);

		} else {
			Log.e(TAG, "Invalid argument type to setData", Log.DEBUG_MODE);
		}
	}
	
	private void handleSetHeaderTitle(String headerTitle) {
		this.headerTitle = headerTitle;
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	private void handleSetFooterTitle(String footerTitle) {
		this.footerTitle = footerTitle;
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	private void handleSetHeaderView(TiViewProxy headerView) {
		this.headerView = headerView;
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	private void handleSetFooterView(TiViewProxy footerView) {
		this.footerView = footerView;
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	
	private void handleAppendItems(Object data) {
		if (data instanceof Object[]) {
			Object[] views = (Object[]) data;
			if (itemProperties == null) {
				itemProperties = new ArrayList<Object>(Arrays.asList(views));
			} else {
				for (Object view: views) {
					itemProperties.add(view);
				}
			}
			//only process items when listview's properties is processed.
			if (getListView() == null) {
				preload = true;
				return;
			}
			//we must update the itemCount before notify data change. If we don't, it will crash
			int count = itemCount;
			itemCount += views.length;

			processData(views, count);
			
		} else {
			Log.e(TAG, "Invalid argument type to setData", Log.DEBUG_MODE);
		}
	}
	
	private void handleInsertItemsAt(int index, Object data) {
		if (data instanceof Object[]) {
			Object[] views = (Object[]) data;
			
			if (itemProperties == null) {
				itemProperties = new ArrayList<Object>(Arrays.asList(views));
			} else {
				if (index < 0 || index > itemProperties.size()) {
					Log.e(TAG, "Invalid index to handleInsertItem", Log.DEBUG_MODE);
					return;
				}
				int counter = index;
				for (Object view: views) {
					itemProperties.add(counter, view);
					counter++;
				}
			}
			//only process items when listview's properties is processed.
			if (getListView() == null) {
				preload = true;
				return;
			}
			
			itemCount += views.length;
			processData(views, index);
		} else {
			Log.e(TAG, "Invalid argument type to insertItemsAt", Log.DEBUG_MODE);
		}
	}
	
	private boolean deleteItems(int index, int count) {
		boolean delete = false;
		while (count > 0) {
			if (index < itemProperties.size()) {
				itemProperties.remove(index);
				itemCount--;
				delete = true;
			}
			if (index < listItemData.size()) {
				listItemData.remove(index);
			}
			count--;
		}
		return delete;
	}
	
	private void handleDeleteItemsAt(int index, int count) {
		deleteItems(index, count);
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}
	
	private void handleReplaceItemsAt(int index, int count, Object data) {
		if (count == 0) {
			handleInsertItemsAt(index, data);
		} else if (deleteItems(index, count)) {
			handleInsertItemsAt(index, data);
		}
	}
	
	private void handleUpdateItemAt(int index, Object data) {
		handleReplaceItemsAt(index, 1, data);
		setProperty(TiC.PROPERTY_ITEMS, itemProperties.toArray());
	}
	
	private CollectionViewTemplate processTemplate(KrollDict itemData, int index) {
		
		CollectionView listView = getListView();
		String defaultTemplateBinding = null;
		if (listView != null) {
			defaultTemplateBinding = listView.getDefaultTemplateBinding();
		}
		//if template is specified in data, we look it up and if one exists, we use it.
		//Otherwise we check if a default template is specified in ListView. If not, we use builtInTemplate.
		String binding = TiConvert.toString(itemData.get(TiC.PROPERTY_TEMPLATE));
		if (binding != null) {
			//check if template is default
			if (binding.equals(UIModule.LIST_ITEM_TEMPLATE_DEFAULT)) {
				return processDefaultTemplate(itemData, index);
			}

			CollectionViewTemplate template = listView.getTemplateByBinding(binding);
			//if template is successfully retrieved, bind it to the index. This is b/c
			//each row can have a different template.
			if (template == null) {
				Log.e(TAG, "Template undefined");
			}
						
			return template;
			
		} else {
			//if a valid default template is specify, use that one
			if (defaultTemplateBinding != null && !defaultTemplateBinding.equals(UIModule.LIST_ITEM_TEMPLATE_DEFAULT)) {
				CollectionViewTemplate defTemplate = listView.getTemplateByBinding(defaultTemplateBinding);
				if (defTemplate != null) {
					return defTemplate;
				}
			}
			return processDefaultTemplate(itemData, index);
		}	
		
	}
	
	private CollectionViewTemplate processDefaultTemplate(KrollDict data, int index) {

		if (builtInTemplate == null){
		
			//Create template and generate default properties
			builtInTemplate = new DefaultCollectionViewTemplate(UIModule.LIST_ITEM_TEMPLATE_DEFAULT, null, getActivity());
			//Each template is treated as an item type, so we can reuse views efficiently.
			//Section templates are given a type in CollectionView.processSections(). Here we
			//give default template a type if possible.
			CollectionView listView = getListView();
			if (listView != null) {
				builtInTemplate.setType(CollectionView.BUILT_IN_TEMPLATE_ITEM_TYPE);
				builtInTemplate.setRootParent(listView.getProxy());
			}
		}

		return builtInTemplate;
	}

	/**
	 * This method creates a new cell and fill it with content. getView() calls this method
	 * when a view needs to be created.
	 * @param index Entry's index relative to its section
	 * @return
	 */
	public void generateCellContent(int sectionIndex, KrollDict data, CollectionViewTemplate template, BaseCollectionViewItem itemContent, int itemPosition, View item_layout) {
		//Here we create an item content and populate it with data
		//Get item proxy
		TiViewProxy itemProxy = template.getRootItem().getViewProxy();
		//Create corresponding TiUIView for item proxy
		CollectionItem item = new CollectionItem(itemProxy, (TiCompositeLayout.LayoutParams)itemContent.getLayoutParams(), itemContent, item_layout);		
		//Connect native view with TiUIView so we can get it from recycled view.
		itemContent.setTag(item);
	
		if (data != null && template != null) {
			generateChildContentViews(template.getRootItem(), null, itemContent, true);
			populateViews(data, itemContent, template, itemPosition, sectionIndex, item_layout);
		}
	}
	
	
	public void generateChildContentViews(DataItem item, TiUIView parentContent, BaseCollectionViewItem rootItem, boolean root) {

		ArrayList<DataItem> childrenItem = item.getChildren();
		for (int i = 0; i < childrenItem.size(); i++) {
			DataItem child = childrenItem.get(i);
			TiViewProxy proxy = child.getViewProxy();
			TiUIView view = proxy.createView(proxy.getActivity());
			view.registerForTouch();
			proxy.setView(view);
			generateChildContentViews(child, view, rootItem, false);
			//Bind view to root.
			
			ViewItem viewItem = new ViewItem(view, new KrollDict());
			rootItem.bindView(child.getBindingId(), viewItem);
			//Add it to view hierarchy
			if (root) {
				rootItem.addView(view.getNativeView(), view.getLayoutParams());
			} else {
				parentContent.add(view);
			}

		}
	}
	
	public void appendExtraEventData(TiUIView view, int itemIndex, int sectionIndex, String bindId, String itemId) {
		KrollDict existingData = view.getAdditionalEventData();
		if (existingData == null) {
			existingData = new KrollDict();
			view.setAdditionalEventData(existingData);
		}

		existingData.put(TiC.PROPERTY_SECTION, this);
		existingData.put(TiC.PROPERTY_SECTION_INDEX, sectionIndex);
		int realItemIndex = itemIndex;
		if (isFilterOn()) {
			realItemIndex =  filterIndices.get(itemIndex);
		}
		existingData.put(TiC.PROPERTY_ITEM_INDEX, realItemIndex);

		if (!bindId.startsWith(CollectionViewTemplate.GENERATED_BINDING) && !bindId.equals(TiC.PROPERTY_PROPERTIES)) {
			existingData.put(TiC.PROPERTY_BIND_ID, bindId);
		} else if (existingData.containsKey(TiC.PROPERTY_BIND_ID)){
			existingData.remove(TiC.PROPERTY_BIND_ID);
		}

		if (itemId != null) {
			existingData.put(TiC.PROPERTY_ITEM_ID, itemId);
		} else if (existingData.containsKey(TiC.PROPERTY_ITEM_ID)){
			existingData.remove(TiC.PROPERTY_ITEM_ID);
		}

	}
	
	public void populateViews(KrollDict data, BaseCollectionViewItem cellContent, CollectionViewTemplate template, int itemIndex, int sectionIndex, View item_layout) {
		Object cell = cellContent.getTag();
		//Handling root item, since that is not in the views map.
		if (!(cell instanceof CollectionItem)) {
			Log.e(TAG, "Cell is not CollectionItem. Something is wrong..", Log.DEBUG_MODE);
			return;
		}
		
		CollectionItem listItem = (CollectionItem) cell;
		KrollDict listItemProperties;
		String itemId = null;

		if (data != null && data.containsKey(TiC.PROPERTY_PROPERTIES)) {
			listItemProperties = new KrollDict((HashMap)data.get(TiC.PROPERTY_PROPERTIES));
		} else {
			listItemProperties = template.getRootItem().getDefaultProperties(); 
		}
		
		//find out if we need to update itemId
		if (listItemProperties.containsKey(TiC.PROPERTY_ITEM_ID)) {
			itemId = TiConvert.toString(listItemProperties.get(TiC.PROPERTY_ITEM_ID));
		}
		
		//update extra event data for list item
		appendExtraEventData(listItem, itemIndex, sectionIndex, TiC.PROPERTY_PROPERTIES, itemId);
		
		HashMap<String, ViewItem> views = (HashMap<String, ViewItem>) cellContent.getViewsMap();
		//Loop through all our views and apply default properties
		for (String binding: views.keySet()) {
			DataItem dataItem = template.getDataItem(binding);
			ViewItem viewItem = views.get(binding);
			TiUIView view = viewItem.getView();
			//update extra event data for views
			if (view != null) {
				appendExtraEventData(view, itemIndex, sectionIndex, binding, itemId);
			}
			//if binding is contain in data given to us, process that data, otherwise
			//apply default properties.
			if (data != null && data.containsKey(binding) && view != null) {
				KrollDict properties = new KrollDict((HashMap)data.get(binding));
				KrollDict diffProperties = viewItem.generateDiffProperties(properties);
				if (!diffProperties.isEmpty()) {
					view.processProperties(diffProperties);
				}

			} else if (dataItem != null && view != null) {
				KrollDict diffProperties = viewItem.generateDiffProperties(dataItem.getDefaultProperties());
				if (!diffProperties.isEmpty()) {
					view.processProperties(diffProperties);
				}
			} else {
				Log.w(TAG, "Sorry, " + binding + " isn't a valid binding. Perhaps you made a typo?", Log.DEBUG_MODE);
			}
			
		}
		
		//process listItem properties
		KrollDict listItemDiff = cellContent.getViewItem().generateDiffProperties(listItemProperties);
		if (!listItemDiff.isEmpty()) {
			listItem.processProperties(listItemDiff);
		}

	}
	
	public CollectionViewTemplate getTemplateByIndex(int index) {
		if (headerTitle != null || headerView != null) {
			index -= 1;
		}

		
		if (isFilterOn()) {
			return listItemData.get(filterIndices.get(index)).getTemplate();
		} else {
			return listItemData.get(index).getTemplate();
		}
	}

	public int getContentCount() {
		if (isFilterOn()) {
			return filterIndices.size();
		} else {
			return itemCount;
		}
	}

	/**
	 * @return number of entries within section
	 */
	public int getItemCount() {
		int totalCount = 0;

		if (isFilterOn()) {
			totalCount = filterIndices.size();
		} else { 
			totalCount = itemCount;
		}
		
		if (!hideHeaderOrFooter()) {
			if (headerTitle != null || headerView != null) {
				totalCount += 1;
			}
			if (footerTitle != null || footerView != null) {
				totalCount +=1;
			}
		}


		return totalCount;
	}
	
	private boolean hideHeaderOrFooter() {
		CollectionView listview = getListView();
		return (listview.getSearchText() != null && filterIndices.isEmpty());
	}
	
	public boolean isHeaderView(int pos) {
		return (headerView != null && pos == 0);
	}
	
	public boolean isFooterView(int pos) {
		return (footerView != null && pos == getItemCount() - 1);
	}

	public boolean isHeaderTitle(int pos) {
		return (headerTitle != null && pos == 0) ;
	}
	
	public boolean isFooterTitle(int pos) {
		return (footerTitle != null && pos == getItemCount() - 1);
	}
	

	
	public void setListView(CollectionView l) {
		listView = new WeakReference<CollectionView>(l);
	}
	
	public CollectionView getListView() {
		if (listView != null) {
			return listView.get();
		}
		return null;
	}
	
	/**
	 * Attempt to give each existing template a type, if possible
	 */
	public void setTemplateType() {
		
		for (int i = 0; i < listItemData.size(); i++) {
			CollectionViewTemplate temp = listItemData.get(i).getTemplate();
			CollectionView listView = getListView();
			if (temp.getType() == -1) {
				temp.setType(listView.getItemType());
			}
		}
	}
	
	public KrollDict getCollectionItemData(int position) {		
		if (isFilterOn()) {
			return listItemData.get(filterIndices.get(position)).getProperties();
		} else if (position >= 0 && position < listItemData.size()) {
			return listItemData.get(position).getProperties();
		} 
		return null;
	}
	
	public boolean isFilterOn() {
		if (getListView().getSearchText() != null) {
			return true;
		}
		return false;
	}

	public void applyFilter(String searchText) {
		//Clear previous result
		filterIndices.clear();
		boolean caseInsensitive = getListView().getCaseInsensitive();
		//Add new results
		for (int i = 0; i < listItemData.size(); ++i) {
			String searchableText = listItemData.get(i).getSearchableText();
			//Handle case sensitivity
			if (caseInsensitive) {
				searchText = searchText.toLowerCase();
				searchableText = searchableText.toLowerCase();
			}
			//String comparison
			if (searchableText.contains(searchText)) {
				filterIndices.add(i);
			}
		}
	}
	
	public void release() {
		if (listItemData != null) {
			listItemData.clear();
			listItemData = null;
		}
		
		if (itemProperties != null) {
			itemProperties.clear();
			itemProperties = null;
		}
		
		if (builtInTemplate != null) {
			builtInTemplate.release();
			builtInTemplate = null;
		}
		super.release();
	}
	
	public void releaseViews()
	{
		listView = null;
	}
	
	@Override
	public String getApiName()
	{
		return "ti.CollectionSection";
	}
}
