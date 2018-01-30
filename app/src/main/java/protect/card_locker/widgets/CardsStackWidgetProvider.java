package protect.card_locker.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import protect.card_locker.R;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link CardsStackWidgetProviderConfigureActivity CardsStackWidgetProviderConfigureActivity}
 */
public class CardsStackWidgetProvider extends AppWidgetProvider
{
    // TODO : Cleanup
    // TODO : Actions on touch (full contrast possible from widget ? open app ?)

//    public static final String EXTRA_ITEM = "protect.card_locker.widgets.cards_stack_widget_provider.EXTRA_ITEM";
//    private static HandlerThread sWorkerThread;
//    private static Handler sWorkerQueue;
//
//    public CardsStackWidgetProvider()
//    {
//        sWorkerThread = new HandlerThread("CardsStackWidgetProvider-worker");
//        sWorkerThread.start();
//        sWorkerQueue = new Handler(sWorkerThread.getLooper());
//    }

    static void updateAppWidget(Context context, final AppWidgetManager appWidgetManager,
                                final int appWidgetId)
    {

//        CharSequence widgetText = CardsStackWidgetProviderConfigureActivity.loadTitlePref(context, appWidgetId);
//        // Construct the RemoteViews object
//        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.cards_stack_widget_provider);
//        views.setTextViewText(R.id.appwidget_text, widgetText);
//
//        // Instruct the widget manager to update the widget
//        appWidgetManager.updateAppWidget(appWidgetId, views);


        // Set up the intent that starts the StackViewService, which will
        // provide the views for this collection.
        Intent intent = new Intent(context, CardsStackWidgetService.class);
        // Add the app widget ID to the intent extras.
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        // Instantiate the RemoteViews object for the app widget layout.
        final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.cards_stack_widget_provider);
        // Set up the RemoteViews object to use a RemoteViews adapter.
        // This adapter connects
        // to a RemoteViewsService  through the specified intent.
        // This is how you populate the data.
        rv.setRemoteAdapter(R.id.stack_view, intent);

        // The empty view is displayed when the collection has no items.
        // It should be in the same layout used to instantiate the RemoteViews
        // object above.
        rv.setEmptyView(R.id.stack_view, R.id.empty_view);

        //
        // Do additional processing specific to this app widget...
        //

//        sWorkerQueue.postDelayed(new Runnable() {
//
//            @Override
//            public void run() {
//                rv.setScrollPosition(R.id.stack_view, 0);
//  //              appWidgetManager.partiallyUpdateAppWidget(appWidgetId, rv);
//                appWidgetManager.updateAppWidget(appWidgetId, rv);
//            }
//
//        }, 3000);

        appWidgetManager.updateAppWidget(appWidgetId, rv);

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds)
        {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds)
    {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds)
        {
            CardsStackWidgetProviderConfigureActivity.deleteTitlePref(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context)
    {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context)
    {
        // Enter relevant functionality for when the last widget is disabled
    }
}

