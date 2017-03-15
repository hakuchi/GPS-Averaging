/*
 * Copyright 2015 David Vávra
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

package com.cytrack.meterlocation.dagger;

import com.cytrack.meterlocation.MainActivity;
import com.cytrack.meterlocation.measure.PeriodicService;
import com.cytrack.meterlocation.ui.fragment.AboutFragment;
import com.cytrack.meterlocation.ui.fragment.MainFragment;
import com.cytrack.meterlocation.ui.view.LocationCardView;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Main Dagger component. Add methods here if you need field injection.
 *
 * @author David Vávra (david@vavra.me)
 */
@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {
    void injectToMainActivity(MainActivity mainActivity);

    void injectToAveragingService(PeriodicService periodicService);

    void injectToLocationCardView(LocationCardView locationCardView);

    void injectToMainFragment(MainFragment mainFragment);

    void injectToAboutFragment(AboutFragment aboutFragment);
}
