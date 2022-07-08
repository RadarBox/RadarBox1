package org.rdr.radarbox.Radargram;

import android.content.Context;
import android.view.SurfaceView;

/** Класс с двумерным полотном, содержащим сетку и отображающим данные в формате Waterfall
 * Данные приходят в формате двумерных массивов. Для них задаются пределы по X и по Y.
 */
public class RadargramSurface extends SurfaceView {

    public RadargramSurface(Context context) {
        super(context);
    }
}
