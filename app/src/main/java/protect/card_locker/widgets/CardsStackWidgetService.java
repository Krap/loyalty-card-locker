package protect.card_locker.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import protect.card_locker.DBHelper;
import protect.card_locker.LoyaltyCard;
import protect.card_locker.R;

public class CardsStackWidgetService extends RemoteViewsService
{
    @Override
    public CardsStackRemoteViewsFactory onGetViewFactory(Intent intent)
    {
        return new CardsStackRemoteViewsFactory(this.getApplicationContext(), intent);
    }


    class CardsStackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory
    {
        private static final int mCount = 10;
        private Context mContext;
        private int mAppWidgetId;
        private DBHelper db;

        CardsStackRemoteViewsFactory(Context context, Intent intent)
        {
            mContext = context;
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                                              AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        public void onCreate()
        {
            // In onCreate() you setup any connections / cursors to your data source. Heavy lifting,
            // for example downloading or creating content etc, should be deferred to onDataSetChanged()
            // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.

            db = new DBHelper(mContext);
        }

        public void onDestroy()
        {
            // In onDestroy() you should tear down anything that was setup for your data source,
            // eg. cursors, connections, etc.
        }

        public int getCount()
        {
            return db.getLoyaltyCardCount();
        }

        public RemoteViews getViewAt(int position)
        {
            Cursor c = db.getLoyaltyCardCursor();
            if (c.moveToPosition(position))
            {
                LoyaltyCard card = LoyaltyCard.toLoyaltyCard(c);

                RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_card);
                rv.setTextViewText(R.id.storeNameView, card.store);
                rv.setTextViewText(R.id.cardIdView, card.cardId);

                // TODO : Factorize
                final int MAX_WIDTH = 500;
                final String cardId;
                BarcodeFormat format;
                final int imageHeight;
                final int imageWidth;

                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
                Bundle options = appWidgetManager.getAppWidgetOptions(mAppWidgetId);
                int imageViewWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
                int imageViewHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

                DisplayMetrics metrics = new DisplayMetrics();
                WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                wm.getDefaultDisplay().getMetrics(metrics);

                rv.setTextViewText(R.id.cardIdView, card.cardId + " " + imageViewWidth + " " + imageViewHeight);

                if (imageViewWidth < MAX_WIDTH)
                {
                    imageHeight = (int) (imageViewHeight * metrics.density);
                    imageWidth = (int) (imageViewWidth * metrics.density);
                }
                else
                {
                    // Scale down the image to reduce the memory needed to produce it
                    imageWidth = (int) (MAX_WIDTH * metrics.density);
                    double ratio = (double) MAX_WIDTH / (double) imageViewWidth;
                    imageHeight = (int) (imageViewHeight * ratio * metrics.density);
                }

                cardId = card.cardId;
                format = BarcodeFormat.valueOf(card.barcodeType);
                final String TAG = "LoyaltyCardLocker";

                MultiFormatWriter writer = new MultiFormatWriter();
                BitMatrix bitMatrix;
                try
                {
                    try
                    {
                        bitMatrix = writer.encode(cardId, format, imageWidth, imageHeight, null);
                    }
                    catch (Exception e)
                    {
                        // Cast a wider net here and catch any exception, as there are some
                        // cases where an encoder may fail if the data is invalid for the
                        // barcode type. If this happens, we want to fail gracefully.
                        throw new WriterException(e);
                    }

                    final int WHITE = 0xFFFFFFFF;
                    final int BLACK = 0xFF000000;

                    int bitMatrixWidth = bitMatrix.getWidth();
                    int bitMatrixHeight = bitMatrix.getHeight();

                    int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

                    for (int y = 0; y < bitMatrixHeight; y++)
                    {
                        int offset = y * bitMatrixWidth;
                        for (int x = 0; x < bitMatrixWidth; x++)
                        {
                            int color = bitMatrix.get(x, y) ? BLACK : WHITE;
                            pixels[offset + x] = color;
                        }
                    }
                    Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight,
                                                        Bitmap.Config.ARGB_8888);
                    bitmap.setPixels(pixels, 0, bitMatrixWidth, 0, 0, bitMatrixWidth, bitMatrixHeight);

                    // Determine if the image needs to be scaled.
                    // This is necessary because the datamatrix barcode generator
                    // ignores the requested size and returns the smallest image necessary
                    // to represent the barcode. If we let the ImageView scale the image
                    // it will use bi-linear filtering, which results in a blurry barcode.
                    // To avoid this, if scaling is needed do so without filtering.

                    int heightScale = imageHeight / bitMatrixHeight;
                    int widthScale = imageWidth / bitMatrixHeight;
                    int scalingFactor = Math.min(heightScale, widthScale);

                    if (scalingFactor > 1)
                    {
                        bitmap = Bitmap.createScaledBitmap(bitmap, bitMatrixWidth * scalingFactor, bitMatrixHeight * scalingFactor, false);
                    }
                    rv.setImageViewBitmap(R.id.barcode, bitmap);
                }
                catch (WriterException e)
                {
                    Log.e(TAG, "Failed to generate barcode of type " + format + ": " + cardId, e);
                }

                // TODO : Background load barcode...

//                // Next, we set a fill-intent which will be used to fill-in the pending intent template
//                // which is set on the collection view in StackWidgetProvider.
//                Bundle extras = new Bundle();
//                extras.putInt(CardsStackWidgetProvider.EXTRA_ITEM, position);
//                Intent fillInIntent = new Intent();
//                fillInIntent.putExtras(extras);
//                rv.setOnClickFillInIntent(R.id.widget_card, fillInIntent);

                return rv;
            }
            else
            {
                // TODO : The asked card doesn't exist...
                return null;
            }


//            // You can do heaving lifting in here, synchronously. For example, if you need to
//            // process an image, fetch something from the network, etc., it is ok to do it here,
//            // synchronously. A loading view will show up in lieu of the actual contents in the
//            // interim.
//            try
//            {
//                System.out.println("Loading view " + position);
//                Thread.sleep(500);
//            }
//            catch (InterruptedException e)
//            {
//                e.printStackTrace();
//            }
//
//            // Return the remote views object.
//            return rv;
        }

        public RemoteViews getLoadingView()
        {
            // You can create a custom loading view (for instance when getViewAt() is slow.) If you
            // return null here, you will get the default loading view.
            return null;
        }

        public int getViewTypeCount()
        {
            return 1;
        }

        public long getItemId(int position)
        {
            return position;
        }

        public boolean hasStableIds()
        {
            return true;
        }

        public void onDataSetChanged()
        {
            // This is triggered when you call AppWidgetManager notifyAppWidgetViewDataChanged
            // on the collection view corresponding to this factory. You can do heaving lifting in
            // here, synchronously. For example, if you need to process an image, fetch something
            // from the network, etc., it is ok to do it here, synchronously. The widget will remain
            // in its current state while work is being done here, so you don't need to worry about
            // locking up the widget.
        }
    }
}
