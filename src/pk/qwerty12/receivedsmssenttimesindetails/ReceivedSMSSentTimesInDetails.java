package pk.qwerty12.receivedsmssenttimesindetails;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;

public class ReceivedSMSSentTimesInDetails implements IXposedHookLoadPackage {

	private static final String PACKAGE_MMS = "com.android.mms";

	private static final String BASE_ID = "_id";
	private static final String MESSAGE_TYPE = "type";
	private static final String DATE_SENT = "date_sent";
	private static final int MESSAGE_TYPE_INBOX = 1;

	private static final String SMS_INBOX_CONTENT_URI = "content://sms/inbox";

	private static long getSmsDateSent(Context context, long msgId)
	{
		//Based slightly on code from SMS Popup
		long retval = 0;
		Cursor cursor = context.getContentResolver().query(Uri.parse(SMS_INBOX_CONTENT_URI), new String[] { DATE_SENT },
								     BASE_ID + " = " + msgId, null,
								     null);

		if (cursor != null && cursor.getCount() == 1)
		{
			cursor.moveToFirst();
			retval = cursor.getLong(cursor.getColumnIndexOrThrow(DATE_SENT));
		}
		
		cursor.close();

		return retval;
	}

	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals(PACKAGE_MMS)) {
			try {
				final Class<?> classMessageUtils = XposedHelpers.findClass(PACKAGE_MMS + ".ui.MessageUtils", lpparam.classLoader);
				XposedHelpers.findAndHookMethod(classMessageUtils, "getTextMessageDetails", Context.class, Cursor.class, boolean.class, PACKAGE_MMS + ".ui.MessageItem", Boolean.class,
				
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						Cursor cursor = (Cursor) param.args[1];
						if (cursor.getInt(cursor.getColumnIndexOrThrow(MESSAGE_TYPE)) == MESSAGE_TYPE_INBOX) {
							Context context = (Context) param.args[0];
							final long date_sent = getSmsDateSent(context, cursor.getInt(cursor.getColumnIndexOrThrow(BASE_ID)));
							if (date_sent > 0) {
								final Resources mmsResources = context.getResources();
								String details = (String)param.getResult();
							    details += "\n" + mmsResources.getString(mmsResources.getIdentifier("sent_label", "string", PACKAGE_MMS)) + " " + XposedHelpers.callStaticMethod(classMessageUtils, "formatTimeStampString", context, date_sent, true);
							    param.setResult(details);
							}
						}
					}
				});
			} catch (Throwable t) { XposedBridge.log(t); }
		}
		
	}
	
}
