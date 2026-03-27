/*
 * Copyright (c) 2018 David Boissier.
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

package com.qihoo.finance.lowcode.console.mongo.view.model.navigation;

import com.qihoo.finance.lowcode.design.entity.MongoCollectionNode;
import com.qihoo.finance.lowcode.console.mongo.model.MongoQueryOptions;

import java.util.ArrayList;
import java.util.List;

public class Navigation {

    private final List<WayPoint> wayPoints = new ArrayList<>();

    private WayPoint currentWayPoint = null;

    public void addNewWayPoint(MongoCollectionNode collection, MongoQueryOptions mongoQueryOptions) {
        currentWayPoint = new WayPoint(collection, mongoQueryOptions);
        wayPoints.add(currentWayPoint);
    }

    public WayPoint getCurrentWayPoint() {
        return currentWayPoint;
    }

    public List<WayPoint> getWayPoints() {
        return wayPoints;
    }

    public void moveBackward() {
        if (currentWayPoint != null) {
            int currentWayPointIndex = wayPoints.indexOf(currentWayPoint);
            if (currentWayPointIndex > 0) {
                currentWayPoint = wayPoints.get(currentWayPointIndex - 1);
                wayPoints.remove(currentWayPointIndex);
            }
        }
    }

    public static class WayPoint {
        private final MongoCollectionNode collection;
        private MongoQueryOptions queryOptions;

        WayPoint(MongoCollectionNode collection, MongoQueryOptions queryOptions) {
            this.collection = collection;
            this.queryOptions = queryOptions;
        }

        public String getLabel() {
            return collection.getDatabase() + "/" + collection.getTableName();
        }

        public MongoCollectionNode getCollection() {
            return collection;
        }

        public void setQueryOptions(MongoQueryOptions queryOptions) {
            this.queryOptions = queryOptions;
        }

        public MongoQueryOptions getQueryOptions() {
            return queryOptions;
        }
    }
}
