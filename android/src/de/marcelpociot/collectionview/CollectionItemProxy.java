package de.marcelpociot.collectionview;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;
import android.app.Activity;

@Kroll.proxy(creatableInModule = AndroidcollectionviewModule.class)
public class CollectionItemProxy extends TiViewProxy
{
	protected WeakReference<TiViewProxy> listProxy;

	public TiUIView createView(Activity activity)
	{
		return new CollectionItem(this);
	}

	public void setListProxy(TiViewProxy list)
	{
		listProxy = new WeakReference<TiViewProxy>(list);
	}

	public TiViewProxy getListProxy()
	{
		if (listProxy != null) {
			return listProxy.get();
		}
		return null;
	}

	public boolean fireEvent(final String event, final Object data, boolean bubbles)
	{
		fireItemClick(event, data);
		return super.fireEvent(event, data, bubbles);
	}

	private void fireItemClick(String event, Object data)
	{
		if (event.equals(TiC.EVENT_CLICK) && data instanceof HashMap) {
			KrollDict eventData = new KrollDict((HashMap) data);
			Object source = eventData.get(TiC.EVENT_PROPERTY_SOURCE);
			if (source != null && !source.equals(this) && listProxy != null) {
				TiViewProxy listViewProxy = listProxy.get();
				if (listViewProxy != null) {
					listViewProxy.fireEvent(TiC.EVENT_ITEM_CLICK, eventData);
				}
			}
		}
	}

	@Override
	public boolean hierarchyHasListener(String event)
	{
		// In order to fire the "itemclick" event when the children views are clicked,
		// the children views' "click" events must be fired and bubbled up. (TIMOB-14901)
		if (event.equals(TiC.EVENT_CLICK)) {
			return true;
		}
		return super.hierarchyHasListener(event);
	}

	public void release()
	{
		super.release();
		if (listProxy != null) {
			listProxy = null;
		}
	}

	@Override
	public String getApiName()
	{
		return "ti.CollectionItem";
	}
}
