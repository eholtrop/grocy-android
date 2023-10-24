/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

public class MealPlanEntryConnection extends GroupedListItem implements Parcelable {

  public MealPlanEntryConnection() {
  }

  public MealPlanEntryConnection(Parcel parcel) {

  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {

  }

  public static final Creator<MealPlanEntryConnection> CREATOR = new Creator<>() {

    @Override
    public MealPlanEntryConnection createFromParcel(Parcel in) {
      return new MealPlanEntryConnection(in);
    }

    @Override
    public MealPlanEntryConnection[] newArray(int size) {
      return new MealPlanEntryConnection[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  @NonNull
  @Override
  public String toString() {
    return "MealPlanConnection()";
  }
}
