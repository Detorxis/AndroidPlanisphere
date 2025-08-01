/*
 * Copyright (C) 2020 Timo Engel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.tengel.planisphere;

import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.lang.Math;

enum ObjectType {STAR, PLANET, SUN, MOON, OTHER}

interface ChartObjectInterface
{
    void draw(DrawArea da, Canvas canvas);
}

abstract class ChartObject implements ChartObjectInterface
{
    protected Paint mPaint = new Paint();
    protected Paint mPaintText = new Paint();
    protected Engine mEngine;
    protected double[] mAzEle = {0.0, 0.0};
    protected String mText = null;
    protected String mTextLong = null;
    protected boolean mShowText = false;
    protected ObjectType mType;
    protected double mApparentMagnitude;
    protected float mFontScale;
    protected float mChartX;
    protected float mChartY;

    ChartObject(Engine e)
    {
        mEngine = e;
        mPaintText.setTextAlign(Paint.Align.CENTER);
        mFontScale = Settings.instance().getFontScale();
    }

    public double getAzimuth()
    {
        return mAzEle[0];
    }

    public double getElevation()
    {
        return mAzEle[1];
    }

    public String getText()
    {
        return mText;
    }

    public String getTextLong()
    {
        if (mTextLong == null)
        {
            return getText();
        }
        else
        {
            return mTextLong;
        }
    }

    public ObjectType getType()
    {
        return mType;
    }

    public String getTypeString()
    {
        int sid = R.string.unknown;
        switch (mType)
        {
            case STAR:
                sid = R.string.star;
                break;
            case PLANET:
                sid = R.string.planet;
                break;
            case SUN:
                sid = R.string.sun;
                break;
            case MOON:
                sid = R.string.moon;
                break;
        }
        return mEngine.getActivity().getString(sid);
    }

    public double getApparentMagnitude()
    {
        return mApparentMagnitude;
    }

    public float getChartX()
    {
        return mChartX;
    }

    public float getChartY()
    {
        return mChartY;
    }
}

//-----------------------------------------------------------------------------

class AzGrid extends ChartObject
{
    public static int sColor;
    private float mAlignY;

    public AzGrid(Engine e)
    {
        super(e);
        mPaint.setColor(sColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaintText.setColor(sColor);
        mPaintText.setTextSize(Settings.instance().getTextSize() * mFontScale);
        mAlignY = (mPaintText.ascent() + mPaintText.descent()) / 2;
        mType = ObjectType.OTHER;
    }

    @Override
    public void draw(DrawArea da, Canvas canvas)
    {
        int[] center  = da.horizontal2area(0, 90);
        int[] pxy, pxyText;
        for (int az = 0; az < 360; az+=45)
        {
            pxy  = da.horizontal2area(az, 0);
            canvas.drawLine(center[0], center[1], pxy[0], pxy[1], mPaint);
            canvas.drawText(Integer.valueOf(az) + "°", pxy[0], pxy[1] - mAlignY,
                            mPaintText);
        }
        for (int ele = 0; ele < 90; ele+=30)
        {
            pxy  = da.horizontal2area(0, ele);
            pxyText  = da.horizontal2area(22.5, ele);
            float radius = (float) Math.sqrt(
                (pxy[1] - center[1]) * (pxy[1] - center[1]) +
                (pxy[0] - center[0]) * (pxy[0] - center[0]));
            canvas.drawCircle(center[0], center[1], radius, mPaint);
            canvas.drawText(Integer.valueOf(ele) + "°", pxyText[0], pxyText[1] - mAlignY, mPaintText);
        }
    }
}

//-----------------------------------------------------------------------------

class Horizon extends ChartObject
{
    public static int sColor = 0;
    private float mAlignY;
    private float mH;

    public Horizon(Engine e)
    {
        super(e);
        mPaint.setColor(sColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(e.getActivity().getResources().getDimension(R.dimen.horizon_width));
        mPaintText.setColor(sColor);
        mPaintText.setTextSize(Settings.instance().getTextSize() * mFontScale);
        mH = mPaintText.ascent() + mPaintText.descent();
        mAlignY = mH / 2;
        mType = ObjectType.OTHER;
    }

    @Override
    public void draw(DrawArea da, Canvas canvas)
    {
        int[] center = da.horizontal2area(0, 90);
        int[] south = da.horizontal2area(0, 0);
        float radius = (float) Math.sqrt(
            (south[1] - center[1]) * (south[1] - center[1]) +
            (south[0] - center[0]) * (south[0] - center[0]));
        canvas.drawCircle(center[0], center[1], radius, mPaint);
        int[] west  = da.horizontal2area(90, 0);
        int[] north = da.horizontal2area(180, 0);
        int[] east  = da.horizontal2area(270, 0);
        double rotateRad = Math.toRadians(Settings.instance().getViewDirection());
        int xOffS = (int) (2 * mH * Math.sin(rotateRad));
        int xOffW = (int) (2 * mH * Math.sin(rotateRad + Math.PI / 2));
        int xOffN = (int) (2 * mH * Math.sin(rotateRad + Math.PI));
        int xOffE = (int) (2 * mH * Math.sin(rotateRad + Math.PI / 2 * 3));
        int yOffS = (int) (mAlignY + 2 * mH * Math.cos(rotateRad));
        int yOffW = (int) (mAlignY + 2 * mH * Math.cos(rotateRad + Math.PI / 2));
        int yOffN = (int) (mAlignY + 2 * mH * Math.cos(rotateRad + Math.PI));
        int yOffE = (int) (mAlignY + 2 * mH * Math.cos(rotateRad + Math.PI / 2 * 3));
        canvas.drawText(da.getContext().getString(R.string.direction_s),
                        south[0] - xOffS, south[1] - yOffS, mPaintText);
        canvas.drawText(da.getContext().getString(R.string.direction_w),
                        west[0] - xOffW, west[1] - yOffW, mPaintText);
        canvas.drawText(da.getContext().getString(R.string.direction_n),
                        north[0] - xOffN, north[1] - yOffN, mPaintText);
        canvas.drawText(da.getContext().getString(R.string.direction_e),
                        east[0] - xOffE, east[1] - yOffE, mPaintText);
    }
}

//-----------------------------------------------------------------------------

class InfoText extends ChartObject
{
    public static int sColor = 0;

    public InfoText(Engine e, String text)
    {
        super(e);
        mText = text;
        mShowText = true;
        mPaintText.setColor(sColor);
        mPaintText.setTextSize(Settings.instance().getTextSizeSmall() * mFontScale);
        mPaintText.setTextAlign(Paint.Align.LEFT);
        mType = ObjectType.OTHER;
    }

    @Override
    public void draw(DrawArea da, Canvas canvas)
    {
        canvas.drawText(mText, 0, mPaintText.ascent() * -1, mPaintText);
    }
}

//-----------------------------------------------------------------------------

abstract class RoundObject extends ChartObject
{
    private float mBaseSize;

    RoundObject(Engine e)
    {
        super(e);
        mBaseSize = Settings.instance().getStarSize();
        mPaintText.setTextAlign(Paint.Align.LEFT);
        mPaintText.setTextSize(Settings.instance().getTextSizeSmall() * mFontScale);
    }

    @Override
    public void draw(DrawArea da, Canvas canvas)
    {
        int[] center = da.horizontal2area(mAzEle[0], mAzEle[1]);
        mChartX = center[0];
        mChartY = center[1];
        float radius;

        if (mApparentMagnitude <= -20)
        {
            radius = 9 * mBaseSize; // <= -20
        }
        else if (mApparentMagnitude > -20 && mApparentMagnitude <= -10.0)
        {
            radius = 7 * mBaseSize; // -20 - -10
        }
        else if (mApparentMagnitude > -10 && mApparentMagnitude <= -3.0)
        {
            radius = 5 * mBaseSize; // -10 - -3
        }
        else if (mApparentMagnitude > -3.0 && mApparentMagnitude <= -1.0)
        {
            radius = 4 * mBaseSize; // -3 - -1
        }
        else if (mApparentMagnitude > -1.0 && mApparentMagnitude <= 1.0)
        {
            radius = 3 * mBaseSize; // -1 - 1
        }
        else if (mApparentMagnitude > 1.0 && mApparentMagnitude <= 3.0)
        {
            radius = 2 * mBaseSize; // 1 - 3
        }
        else if (mApparentMagnitude > 3.0 && mApparentMagnitude <= 5.0)
        {
            radius = 1 * mBaseSize; // 3 - 5
        }
        else
        {
            radius = 0.5f * mBaseSize;
        }
        canvas.drawCircle(center[0], center[1], radius, mPaint);

        if (mShowText && mText != null)
        {
            canvas.drawText(mText, center[0] + radius, center[1] , mPaintText);
        }
    }
}

//-----------------------------------------------------------------------------

class Star extends RoundObject
{
    public static int sColor;
    public static int sTextColor;
    private Catalog.Entry mEntry;

    public Star(Engine engine, Catalog.Entry ce, boolean isNamesEnabled)
    {
        super(engine);
        mEntry = ce;
        mAzEle = mEngine.equatorial2horizontal(ce.rightAscension, ce.declination);
        mApparentMagnitude = ce.apparentMagnitude;
        mPaint.setColor(sColor);
        mPaintText.setColor(sTextColor);

        if (mEntry.name != null)
        {
            mText = mEntry.name;
            mTextLong = mEntry.name;
            if (mEntry.bayerFlamsteed.length() > 0)
            {
                mTextLong += " (" + mEntry.bayerFlamsteed + ")";
            }
            mShowText = isNamesEnabled;
        }
        else
        {
            mText = "HR " + mEntry.hr;
            if (mEntry.bayerFlamsteed.length() > 0)
            {
                mText += " (" + mEntry.bayerFlamsteed + ")";
            }
        }
        mType = ObjectType.STAR;
    }

    public Catalog.Entry getCatalogEntry()
    {
        return mEntry;
    }
}

//-----------------------------------------------------------------------------

class ChartPlanet extends RoundObject
{
    public static int sColor;
    public static int sTextColor;
    private Planet mPlanet;

    public ChartPlanet(Engine e, Planet planet, boolean showName)
    {
        super(e);
        mPlanet = planet;
        mAzEle = mEngine.equatorial2horizontal(planet.mRa / 15, planet.mDeclination);
        mApparentMagnitude = mPlanet.mApparentMagnitude;
        mText = Settings.instance().translateName(mPlanet.mName);
        mShowText = showName;
        mPaint.setColor(sColor);
        mPaintText.setColor(sTextColor);
        mType = ObjectType.PLANET;
    }

    public Planet getPlanet()
    {
        return mPlanet;
    }
}

//-----------------------------------------------------------------------------

class Sun extends RoundObject
{
    public static int sColor;
    public static int sTextColor;
    public static final String sWikidataId = "Q525";
    public double mRa;
    public double mDeclination;
    public double mEcliptic_lon;

    public Sun(Engine e, boolean showName)
    {
        super(e);
        double[] latLonDist = Astro.calcPositionSunEcliptic(
            Astro.julian_date(mEngine.getTime()));
        mEcliptic_lon = latLonDist[1];
        double[] raDec = Astro.geoEcl2geoEqua(latLonDist[0], latLonDist[1]);
        mRa = raDec[0];
        mDeclination = raDec[1];
        mAzEle = mEngine.equatorial2horizontal(raDec[0] / 15, raDec[1]);
        mApparentMagnitude = -26.74;
        mText = Settings.instance().translateName("Sun");
        mShowText = showName;
        mPaint.setColor(sColor);
        mPaintText.setColor(sTextColor);
        mType = ObjectType.SUN;
    }
}

//-----------------------------------------------------------------------------

class Moon extends RoundObject
{
    public static int sColor;
    public static int sTextColor;
    public static final String sWikidataId = "Q405";
    public double mPhase;
    public double mDistance_sun;
    public double mDistance_earth;

    public Moon(Engine e, boolean showName)
    {
        super(e);
        double[] geoEclPos;   // beta (lat), lambda (lon), Delta (earth distance)
        double[] helioEclPos; // b (lat), l (lon), r (distance sun)
        double[] raDec;

        geoEclPos = Astro.calcPositionMoon(Astro.julian_date(mEngine.getTime()));
        mDistance_earth = geoEclPos[2];
        raDec = Astro.geoEcl2geoEqua(geoEclPos[0], geoEclPos[1]);

        mAzEle = mEngine.equatorial2horizontal(raDec[0] / 15, raDec[1]);
        mApparentMagnitude = -12.7;
        mText = Settings.instance().translateName("Moon");
        mShowText = showName;
        mPaint.setColor(sColor);
        mPaintText.setColor(sTextColor);
        mType = ObjectType.MOON;

        helioEclPos = Astro.geoEcl2helioEcl(
                Planet.sEarth.mHelio_lon, Planet.sEarth.mHelio_lat, Planet.sEarth.mDistance_sun,
                geoEclPos[1], geoEclPos[0], geoEclPos[2]);
        mDistance_sun = helioEclPos[2];
        mPhase = Astro.calcPhase(mDistance_earth, mDistance_sun, Planet.sEarth.mDistance_sun);
    }
}

//=============================================================================

abstract class LineObject extends ChartObject
{
    protected ArrayList<ArrayList<double[]>> mLines = new ArrayList<>();
    protected ArrayList<double[]> mTextCoords = new ArrayList<double[]>();
    protected ArrayList<String> mTexts = new ArrayList<String>();
    protected ArrayList<String> mTextsCenter = new ArrayList<String>();
    protected boolean mShowLines = true;

    public LineObject(Engine e)
    {
        super(e);
    }

    @Override
    public void draw(DrawArea da, Canvas canvas)
    {
        if (mShowLines)
        {
            for (ArrayList<double[]> line : mLines)
            {
                drawLine(da, canvas, line);
            }
        }

        int[] pxy;
        Iterator<double[]> coordIter = mTextCoords.iterator();
        Iterator<String> textIter = mTexts.iterator();
        while(coordIter.hasNext() && textIter.hasNext())
        {
            pxy = da.horizontal2area(coordIter.next());
            canvas.drawText(textIter.next(), pxy[0], pxy[1], mPaintText);
        }

        Iterator<ArrayList<double[]>> lineIter = mLines.iterator();
        Iterator<String> textCenterIter = mTextsCenter.iterator();
        while (lineIter.hasNext() && textCenterIter.hasNext())
        {
            float xMax = 0, xMin = Float.MAX_VALUE;
            float yMax = 0, yMin = Float.MAX_VALUE;
            for (double[] point : lineIter.next())
            {
                int[] pointXy = da.horizontal2area(point);
                xMax = Math.max(xMax, pointXy[0]);
                xMin = Math.min(xMin, pointXy[0]);
                yMax = Math.max(yMax, pointXy[1]);
                yMin = Math.min(yMin, pointXy[1]);
            }
            canvas.drawText(textCenterIter.next(),
                            xMin + ((xMax - xMin) / 2),
                            yMin + ((yMax - yMin) / 2), mPaintText);
        }
    }

    private void drawLine(DrawArea da, Canvas canvas, ArrayList<double[]> line)
    {
        int[] pFrom;
        int[] pTo;
        Iterator<double[]> iterator = line.iterator();
        pFrom = da.horizontal2area(iterator.next());
        while (iterator.hasNext())
        {
            pTo = da.horizontal2area(iterator.next());
            canvas.drawLine(pFrom[0], pFrom[1], pTo[0], pTo[1], mPaint);
            pFrom = pTo;
        }
    }
}

//-----------------------------------------------------------------------------

class EqGrid extends LineObject
{
    public static int sColor;

    public EqGrid(Engine e)
    {
        super(e);
        mType = ObjectType.OTHER;
        mPaint.setColor(sColor);
        mPaintText.setColor(sColor);
        mPaintText.setTextSize(Settings.instance().getTextSize() * mFontScale);
        ArrayList<double[]> line;
        for (int dec = -30; dec < 90; dec+=30)
        {
            line = new ArrayList<>();
            for (int ra = 0; ra <= 24; ++ra)
            {
                line.add(mEngine.equatorial2horizontal(ra, dec));
            }
            mLines.add(line);
            mTextCoords.add(mEngine.equatorial2horizontal(1, dec));
            mTexts.add(Integer.valueOf(dec) + "°");
        }
        for ( int ra = 0; ra < 24; ra+=2)
        {
            line = new ArrayList<>();
            for (int dec = -30; dec <= 90; dec+=10)
            {
                line.add(mEngine.equatorial2horizontal(ra, dec));
            }
            mLines.add(line);
            mTextCoords.add(mEngine.equatorial2horizontal(ra, -30));
            mTexts.add(Integer.valueOf(ra) + " h");
        }
    }
}

//-----------------------------------------------------------------------------

class Equator extends LineObject
{
    public static int sColor;

    public Equator(Engine e)
    {
        super(e);
        mType = ObjectType.OTHER;
        mPaint.setColor(sColor);
        ArrayList<double[]> line = new ArrayList<>();
        for (int ra = 0; ra <= 24; ra++)
        {
            line.add(mEngine.equatorial2horizontal(ra, 0));
        }
        mLines.add(line);
    }
}
//-----------------------------------------------------------------------------

class Ecliptic extends LineObject
{
    public static int sColor;

    public Ecliptic(Engine e)
    {
        super(e);
        mType = ObjectType.OTHER;
        mPaint.setColor(sColor);
        mPaintText.setColor(sColor);
        double[] raDec;
        ArrayList<double[]> line = new ArrayList<>();
        for (int lon = 0; lon <= 360; lon+=10)
        {
            raDec = Astro.geoEcl2geoEqua(0.0, lon);
            line.add(mEngine.equatorial2horizontal(raDec[0] / 15, raDec[1]));
        }
        mLines.add(line);
    }
}

//-----------------------------------------------------------------------------

class ConstLines extends LineObject
{
    public static int sColor;

    public ConstLines(Engine e, ConstellationDb db, ConstBoundaries boundaries,
                      boolean isLinesEnabled, boolean isNamesEnabled)
    {
        super(e);
        mType = ObjectType.OTHER;
        mPaint.setColor(sColor);
        mPaintText.setColor(sColor);
        mPaintText.setTextSize(Settings.instance().getTextSizeSmall() * mFontScale);
        Double[] azEle;
        boolean isVisible;
        for (ConstellationDb.Constellation constellation : db.get())
        {
            if (!boundaries.isVisible(constellation.mName))
            {
                continue;
            }
            ArrayList<double[]> constLine = new ArrayList<>();
            for (Catalog.Entry ce : constellation.mLine)
            {
                constLine.add(mEngine.equatorial2horizontal(ce.rightAscension, ce.declination));
            }
            mShowLines = isLinesEnabled;
            mLines.add(constLine);
            if (isNamesEnabled)
            {
                mTextsCenter.add(db.getName(constellation.mName));
            }
        }
    }
}

//-----------------------------------------------------------------------------

class ConstBoundaries extends LineObject
{
    public static int sColor;
    private HashMap<String, Boolean> mVisibility = new HashMap<>();

    public ConstBoundaries(Engine e, ConstellationDb db, boolean isBoundEnabled)
    {
        super(e);
        mType = ObjectType.OTHER;
        mPaint.setColor(sColor);
        double[] azEle;
        double[] azEleFirst = null;
        boolean isVisible;
        for (ConstellationDb.Constellation constellation : db.get())
        {
            ArrayList<double[]> boundLine = new ArrayList<>();
            isVisible = false;
            azEleFirst = null;
            for (Double[] raDec : db.getBoundary(constellation.mName))
            {
                azEle = mEngine.equatorial2horizontal(raDec[0], raDec[1]);
                boundLine.add(azEle);
                if (azEleFirst == null)
                {
                    azEleFirst = azEle;
                }
                if (azEle[1] > 0)
                {
                    isVisible = true;
                }
            }
            boundLine.add(azEleFirst);
            if (isVisible && isBoundEnabled)
            {
                mLines.add(boundLine);
            }
            mVisibility.put(constellation.mName, isVisible);
        }
    }

    public boolean isVisible(String name)
    {
        return mVisibility.get(name);
    }
}
