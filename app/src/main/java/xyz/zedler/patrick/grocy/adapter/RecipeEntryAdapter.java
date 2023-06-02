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
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.ColorRoles;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.RowRecipeEntryBinding;
import xyz.zedler.patrick.grocy.databinding.RowRecipeEntryGridBinding;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataRecipesExtraField;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class RecipeEntryAdapter extends
    RecyclerView.Adapter<RecipeEntryAdapter.ViewHolder> {

  private final static String TAG = RecipeEntryAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private Context context;
  private final LayoutManager layoutManager;
  private final ArrayList<Recipe> recipes;
  private final ArrayList<RecipeFulfillment> recipeFulfillments;
  private final RecipesItemAdapterListener listener;
  private final GrocyApi grocyApi;
  private final LazyHeaders grocyAuthHeaders;
  private String sortMode;
  private boolean sortAscending;
  private String extraField;
  private final int maxDecimalPlacesAmount;
  private boolean containsPictures;
  private RecyclerView recyclerView;

  public RecipeEntryAdapter(
      Context context,
      LayoutManager layoutManager,
      ArrayList<Recipe> recipes,
      ArrayList<RecipeFulfillment> recipeFulfillments,
      RecipesItemAdapterListener listener,
      String sortMode,
      boolean sortAscending,
      String extraField
  ) {
    this.context = context;
    this.layoutManager = layoutManager;
    this.recipes = new ArrayList<>(recipes);
    this.recipeFulfillments = new ArrayList<>(recipeFulfillments);
    this.listener = listener;
    this.grocyApi = new GrocyApi((Application) context.getApplicationContext());
    this.grocyAuthHeaders = RequestHeaders.getGlideGrocyAuthHeaders(context);
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
    this.extraField = extraField;
    maxDecimalPlacesAmount = PreferenceManager.getDefaultSharedPreferences(context).getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );

    containsPictures = false;
    for (Recipe recipe : recipes) {
      String pictureFileName = recipe.getPictureFileName();
      if (pictureFileName != null && !pictureFileName.isEmpty()) {
        containsPictures = true;
        break;
      }
    }
  }

  @Override
  public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onDetachedFromRecyclerView(recyclerView);
    this.context = null;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class RecipeViewHolder extends ViewHolder {

    private final RowRecipeEntryBinding binding;

    public RecipeViewHolder(RowRecipeEntryBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public static class RecipeGridViewHolder extends ViewHolder {

    private final RowRecipeEntryGridBinding binding;
    StaggeredGridLayoutManager.LayoutParams layoutParams;

    public RecipeGridViewHolder(RowRecipeEntryGridBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
      layoutParams = (StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams();
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (layoutManager instanceof LinearLayoutManager) {
      return new RecipeViewHolder(RowRecipeEntryBinding.inflate(
          LayoutInflater.from(parent.getContext()),
          parent,
          false
      ));
    } else {
      return new RecipeGridViewHolder(RowRecipeEntryGridBinding.inflate(
          LayoutInflater.from(parent.getContext()),
          parent,
          false
      ));
    }
  }

  @SuppressLint({"ClickableViewAccessibility", "RestrictedApi"})
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {

    int position = viewHolder.getAdapterPosition();

    Recipe recipe = recipes.get(position);
    RecipeFulfillment recipeFulfillment = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(
        recipeFulfillments, recipe.getId()
    );

    ViewGroup container;
    TextView title;
    ImageView picture;
    MaterialCardView picturePlaceholder;

    FlexboxLayout chips = null;
    ImageView imageFulfillment;
    String textFulfillment = null;

    if (viewHolder instanceof RecipeViewHolder) {
      container = ((RecipeViewHolder) viewHolder).binding.container;
      title = ((RecipeViewHolder) viewHolder).binding.title;
      chips = ((RecipeViewHolder) viewHolder).binding.flexboxLayout;
      picture = ((RecipeViewHolder) viewHolder).binding.picture;
      picturePlaceholder = ((RecipeViewHolder) viewHolder).binding.picturePlaceholder;
    } else {
      container = ((RecipeGridViewHolder) viewHolder).binding.container;
      title = ((RecipeGridViewHolder) viewHolder).binding.title;
      chips = ((RecipeGridViewHolder) viewHolder).binding.flexboxLayout;
      picture = ((RecipeGridViewHolder) viewHolder).binding.picture;
      picturePlaceholder = ((RecipeGridViewHolder) viewHolder).binding.picturePlaceholder;
    }

    // NAME

    title.setText(recipe.getName());

    chips.removeAllViews();

    if (recipeFulfillment != null) {
      ColorRoles colorGreen = ResUtil.getHarmonizedRoles(context, R.color.green);
      ColorRoles colorYellow = ResUtil.getHarmonizedRoles(context, R.color.yellow);
      ColorRoles colorRed = ResUtil.getHarmonizedRoles(context, R.color.red);

      // DUE SCORE
      int due_score = recipeFulfillment.getDueScore();
      @ColorInt int color;

      if (due_score == 0) {
        color = colorGreen.getAccent();
      }
      else if (due_score <= 10) {
        color = colorYellow.getAccent();
      }
      else {
        color = colorRed.getAccent();
      }
      chips.addView(createChip(context, context.getString(
          R.string.subtitle_recipe_due_score,
          String.valueOf(recipeFulfillment.getDueScore())
      ), color));

      // REQUIREMENTS FULFILLED
      Chip chipFulfillment = createChip(context, context.getString(R.string.property_status_insert), -1);
      chipFulfillment.setCloseIconStartPadding(UiUtil.dpToPx(context, 4));
      chipFulfillment.setCloseIconVisible(true);

      if (recipeFulfillment.isNeedFulfilled()) {
        textFulfillment = context.getString(R.string.msg_recipes_enough_in_stock);
        chipFulfillment.setCloseIcon(
            ContextCompat.getDrawable(context, R.drawable.ic_round_check_circle_outline)
        );
        chipFulfillment.setCloseIconTint(ColorStateList.valueOf(colorGreen.getAccent()));
      } else if (recipeFulfillment.isNeedFulfilledWithShoppingList()) {
        textFulfillment = context.getString(R.string.msg_recipes_not_enough) + "\n"
            + context.getResources()
            .getQuantityString(R.plurals.msg_recipes_ingredients_missing_but_on_shopping_list,
                recipeFulfillment.getMissingProductsCount(),
                recipeFulfillment.getMissingProductsCount());
        chipFulfillment.setCloseIcon(
            ContextCompat.getDrawable(context, R.drawable.ic_round_error_outline)
        );
        chipFulfillment.setCloseIconTint(ColorStateList.valueOf(colorYellow.getAccent()));
      } else {
        textFulfillment = context.getString(R.string.msg_recipes_not_enough) + "\n"
            + context.getResources()
            .getQuantityString(R.plurals.msg_recipes_ingredients_missing,
                recipeFulfillment.getMissingProductsCount(),
                recipeFulfillment.getMissingProductsCount());
        chipFulfillment.setCloseIcon(
            ContextCompat.getDrawable(context, R.drawable.ic_round_highlight_off)
        );
        chipFulfillment.setCloseIconTint(ColorStateList.valueOf(colorRed.getAccent()));
      }
      chips.addView(chipFulfillment);
      String finalTextFulfillment = textFulfillment;
      chipFulfillment.setOnClickListener(v -> {
        new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Grocy_AlertDialog)
            .setTitle(R.string.property_requirements_fulfilled)
            .setMessage(finalTextFulfillment)
            .setPositiveButton(R.string.action_close, (dialog, which) -> dialog.dismiss())
            .create().show();
      });
    }

    switch (extraField) {
      case FilterChipLiveDataRecipesExtraField.EXTRA_FIELD_CALORIES:
        if (recipeFulfillment != null) {
          chips.addView(createChip(context, NumUtil.trimAmount(
              recipeFulfillment.getCalories(), maxDecimalPlacesAmount
          ) + " kcal", -1)); // TODO: UNIT
        }
        break;
    }

    String pictureFileName = recipe.getPictureFileName();
    if (pictureFileName != null && !pictureFileName.isEmpty()) {
      picture.layout(0, 0, 0, 0);

      RequestBuilder<Drawable> requestBuilder = Glide.with(context).load(new GlideUrl(grocyApi.getRecipePicture(pictureFileName), grocyAuthHeaders));
      requestBuilder = requestBuilder
          .transform(new CenterCrop())
          .transition(DrawableTransitionOptions.withCrossFade());
      if (viewHolder instanceof RecipeGridViewHolder) {
        requestBuilder = requestBuilder.override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
      }
      requestBuilder.listener(new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                Target<Drawable> target, boolean isFirstResource) {
              picture.setVisibility(View.GONE);
              picturePlaceholder.setVisibility(View.VISIBLE);
              return false;
            }
            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
                DataSource dataSource, boolean isFirstResource) {
              picture.setVisibility(View.VISIBLE);
              picturePlaceholder.setVisibility(View.GONE);
              return false;
            }
          }).into(picture);
    } else if (containsPictures && viewHolder instanceof RecipeViewHolder) {
      picture.setVisibility(View.GONE);
      picturePlaceholder.setVisibility(View.VISIBLE);
    } else {
      picture.setVisibility(View.GONE);
      picturePlaceholder.setVisibility(View.GONE);
    }

    // CONTAINER

    container.setOnClickListener(
        view -> listener.onItemRowClicked(recipe)
    );

    // DIVIDER

    if (layoutManager instanceof StaggeredGridLayoutManager
        && viewHolder instanceof RecipeGridViewHolder) {

    }

  }

  private static Chip createChip(Context ctx, String text, int textColor) {
    @SuppressLint("InflateParams")
    Chip chip = (Chip) LayoutInflater.from(ctx)
        .inflate(R.layout.view_info_chip, null, false);

    chip.setText(text);
    if (textColor != -1) {
      chip.setTextColor(textColor);
    }
    return chip;
  }

  @Override
  public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onAttachedToRecyclerView(recyclerView);
    this.recyclerView = recyclerView;
  }

  private int[] getLastVisiblePositions() {
    if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
      StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
      int spanCount = layoutManager.getSpanCount();
      int[] lastVisiblePositions = new int[spanCount];
      layoutManager.findLastVisibleItemPositions(lastVisiblePositions);
      return lastVisiblePositions;
    }
    return null;
  }



  @Override
  public int getItemCount() {
    return recipes.size();
  }

  public interface RecipesItemAdapterListener {

    void onItemRowClicked(Recipe recipe);
  }

  public void updateData(
      ArrayList<Recipe> newList,
      ArrayList<RecipeFulfillment> newRecipeFulfillments,
      String sortMode,
      boolean sortAscending,
      String extraField
  ) {

    RecipeEntryAdapter.DiffCallback diffCallback = new RecipeEntryAdapter.DiffCallback(
        this.recipes,
        newList,
        this.recipeFulfillments,
        newRecipeFulfillments,
        this.sortMode,
        sortMode,
        this.sortAscending,
        sortAscending,
        this.extraField,
        extraField
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.recipes.clear();
    this.recipes.addAll(newList);
    this.recipeFulfillments.clear();
    this.recipeFulfillments.addAll(newRecipeFulfillments);
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
    this.extraField = extraField;
    diffResult.dispatchUpdatesTo(this);
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<Recipe> oldItems;
    ArrayList<Recipe> newItems;
    ArrayList<RecipeFulfillment> oldRecipeFulfillments;
    ArrayList<RecipeFulfillment> newRecipeFulfillments;
    String sortModeOld;
    String sortModeNew;
    boolean sortAscendingOld;
    boolean sortAscendingNew;
    String extraFieldOld;
    String extraFieldNew;

    public DiffCallback(
        ArrayList<Recipe> oldItems,
        ArrayList<Recipe> newItems,
        ArrayList<RecipeFulfillment> oldRecipeFulfillments,
        ArrayList<RecipeFulfillment> newRecipeFulfillments,
        String sortModeOld,
        String sortModeNew,
        boolean sortAscendingOld,
        boolean sortAscendingNew,
        String extraFieldOld,
        String extraFieldNew
    ) {
      this.oldItems = oldItems;
      this.newItems = newItems;
      this.oldRecipeFulfillments = oldRecipeFulfillments;
      this.newRecipeFulfillments = newRecipeFulfillments;
      this.sortModeOld = sortModeOld;
      this.sortModeNew = sortModeNew;
      this.sortAscendingOld = sortAscendingOld;
      this.sortAscendingNew = sortAscendingNew;
      this.extraFieldOld = extraFieldOld;
      this.extraFieldNew = extraFieldNew;
    }

    @Override
    public int getOldListSize() {
      return oldItems.size();
    }

    @Override
    public int getNewListSize() {
      return newItems.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, false);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, true);
    }

    private boolean compare(int oldItemPos, int newItemPos, boolean compareContent) {
      Recipe newItem = newItems.get(newItemPos);
      Recipe oldItem = oldItems.get(oldItemPos);

      if (!sortModeOld.equals(sortModeNew)) {
        return false;
      }
      if (sortAscendingOld != sortAscendingNew) {
        return false;
      }
      if (!extraFieldOld.equals(extraFieldNew)) {
        return false;
      }

      RecipeFulfillment recipeFulfillmentOld = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(oldRecipeFulfillments, oldItem.getId());
      RecipeFulfillment recipeFulfillmentNew = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(newRecipeFulfillments, newItem.getId());
      if (recipeFulfillmentOld == null && recipeFulfillmentNew != null
          || recipeFulfillmentOld != null && recipeFulfillmentNew == null
          || recipeFulfillmentOld != null && !recipeFulfillmentOld.equals(recipeFulfillmentNew)) {
        return false;
      }

      if (!compareContent) {
        return newItem.getId() == oldItem.getId();
      }

      return newItem.equalsForListDiff(oldItem);
    }
  }
}
