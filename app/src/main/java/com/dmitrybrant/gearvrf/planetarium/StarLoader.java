/* Copyright 2016 Dmitry Brant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dmitrybrant.gearvrf.planetarium;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.gearvrf.utility.Log;

public class StarLoader {
    private static final String TAG = "StarLoader";
    public static final float MAX_STAR_MAGNITUDE = 7f;
    public static final float DEFAULT_DISTANCE_STAR = 500f;
    private static final double STAR_PICK_RADIUS = 0.5;

    private List<SkyObject> starList = new ArrayList<>();

    public StarLoader(Context context) {
        InputStream instream = null;
        try {
            Log.d(TAG, "Loading stars...");
            instream = context.getAssets().open("stars.txt");
            BufferedReader buffreader = new BufferedReader(new InputStreamReader(instream));
            String line;
            String[] lineArr;
            buffreader.readLine();
            while ((line = buffreader.readLine()) != null) {
                lineArr = line.split("\\s+");
                if (lineArr.length < 6) {
                    continue;
                }

                SkyObject s = new SkyObject();
                starList.add(s);
                s.type = SkyObject.TYPE_STAR;
                s.hipNum = Integer.parseInt(lineArr[0]);

                s.ra = Double.parseDouble(lineArr[1]);
                s.dec = Double.parseDouble(lineArr[2]);
                s.dist = (float)Double.parseDouble(lineArr[3]);

                s.mag = Float.parseFloat(lineArr[4]);
                s.className = lineArr[5];
                s.name = "HIP " + s.hipNum;

                // for converting to (x,y,z):
                //s.x = (float) ((s.dist * Math.cos(s.dec)) * Math.cos(s.ra));
                //s.y = (float) ((s.dist * Math.cos(s.dec)) * Math.sin(s.ra));
                //s.z = (float) (s.dist * Math.sin(s.dec));
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read star database.", e);
        } finally {
            if (instream != null) {
                try { instream.close(); instream = null; }
                catch(Exception e) {
                    //
                }
            }
        }
        try {
            Log.d(TAG, "Loading star names...");
            instream = context.getAssets().open("starnames.txt");
            BufferedReader buffreader = new BufferedReader(new InputStreamReader(instream));
            String line;
            String[] lineArr;
            buffreader.readLine();
            while ((line = buffreader.readLine()) != null) {
                lineArr = line.split(":");
                int index = Integer.parseInt(lineArr[0]);
                for (SkyObject star : starList) {
                    if (star.hipNum == index) {
                        star.name = Util.transformStarName(lineArr[1]);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read star names.", e);
        } finally {
            if (instream != null) {
                try { instream.close(); }
                catch(Exception e) {
                    //
                }
            }
        }

        //sort them by RA
        Collections.sort(starList, new Comparator<SkyObject>() {
            @Override
            public int compare(SkyObject lhs, SkyObject rhs) {
                return Double.compare(lhs.ra, rhs.ra);
            }
        });
    }

    public List<SkyObject> getStarList() {
        return starList;
    }

    public SkyObject pickStar(double ra, double dec) {
        double numStars = starList.size();
        int i, j = (int) ((ra / 360.0) * numStars);
        if (j >= starList.size()) { j = starList.size() - 1; }
        if (j < 0) { j = 0; }
        i = j;
        while (i >= 0) {
            if (ra - starList.get(i).ra > STAR_PICK_RADIUS) {
                break;
            }
            if (Math.abs(starList.get(i).dec - dec) <= STAR_PICK_RADIUS) {
                return starList.get(i);
            }
            i--;
        }
        i = j;
        while (i < starList.size()) {
            if (starList.get(i).ra - ra > STAR_PICK_RADIUS) {
                break;
            }
            if (Math.abs(starList.get(i).dec - dec) <= STAR_PICK_RADIUS) {
                return starList.get(i);
            }
            i++;
        }
        return null;
    }
}
