package com.github.yeriomin.yalpstore;

import com.github.yeriomin.yalpstore.model.App;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SearchTask extends GoogleApiAsyncTask {

    protected List<App> apps = new ArrayList<>();
    private Set<String> installedPackageNames = new HashSet<>();
    private CategoryManager categoryManager;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        List<App> installed = UpdatableAppsTask.getInstalledApps(context);
        for (App installedApp : installed) {
            installedPackageNames.add(installedApp.getPackageName());
        }
    }

    /**
     * params[0] is search query
     * params[1] is category id
     *
     */
    @Override
    protected Throwable doInBackground(String... params) {
        PlayStoreApiWrapper wrapper = new PlayStoreApiWrapper(context);
        try {
            AppSearchResultIterator iterator = wrapper.getSearchIterator(params[0], params[1]);
            while (iterator.hasNext() && apps.isEmpty()) {
                getNextBatch(iterator, params[1]);
            }
            for (App app: apps) {
                app.setInstalled(installedPackageNames.contains(app.getPackageName()));
            }
        } catch (Throwable e) {
            return e;
        }
        return null;
    }

    private void getNextBatch(AppSearchResultIterator iterator, String chosenCategoryId) {
        for (App app: iterator.next()) {
            if (categoryManager.fits(app.getCategoryId(), chosenCategoryId)) {
                apps.add(app);
            }
        }
    }

    public void setCategoryManager(CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }
}
