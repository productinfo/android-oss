package com.kickstarter.viewmodels;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.kickstarter.KSApplication;
import com.kickstarter.libs.ViewModel;
import com.kickstarter.libs.rx.transformers.Transformers;
import com.kickstarter.models.Category;
import com.kickstarter.services.ApiClient;
import com.kickstarter.services.DiscoveryParams;
import com.kickstarter.ui.activities.DiscoveryFilterActivity;
import com.kickstarter.ui.adapters.DiscoveryFilterAdapter;
import com.kickstarter.ui.viewholders.DiscoveryFilterViewHolder;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;

public final class DiscoveryFilterViewModel extends ViewModel<DiscoveryFilterActivity> implements DiscoveryFilterAdapter.Delegate {
  @Inject ApiClient apiClient;

  private final PublishSubject<DiscoveryParams> discoveryFilterClick = PublishSubject.create();

  @Override
  protected void onCreate(@NonNull final Context context, @Nullable final Bundle savedInstanceState) {
    super.onCreate(context, savedInstanceState);
    ((KSApplication) context.getApplicationContext()).component().inject(this);

    final Observable<List<Category>> categories = apiClient.fetchCategories()
      .retry(3)
      .compose(Transformers.neverError());

    final Observable<Pair<DiscoveryFilterActivity, List<Category>>> viewAndCategories = view
      .compose(Transformers.combineLatestPair(categories));

    addSubscription(viewAndCategories
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(vc -> vc.first.loadCategories(vc.second)));

    addSubscription(view
        .compose(Transformers.takePairWhen(discoveryFilterClick))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(vp -> vp.first.startDiscoveryActivity(vp.second))
    );

    addSubscription(discoveryFilterClick.subscribe(koala::trackDiscoveryFilterSelected));
    koala.trackDiscoveryFilters();
  }

  public void discoveryFilterClick(@NonNull final DiscoveryFilterViewHolder viewHolder, @NonNull final DiscoveryParams discoveryParams) {
    discoveryFilterClick.onNext(discoveryParams);
  }
}