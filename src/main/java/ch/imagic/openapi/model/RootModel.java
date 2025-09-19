// Copyright 2025 Imagic Bildverarbeitung AG CH-8152 Glattbrugg
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package ch.imagic.openapi.model;

import java.util.Map;

public class RootModel {
    private TagModel[] tags;
    private Map<String, Map<String, PathModel>> paths;
    private ComponentsModel components;

    public Map<String, Map<String, PathModel>> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, Map<String, PathModel>> paths) {
        this.paths = paths;
    }

    public TagModel[] getTags() {
        return tags;
    }

    public void setTags(TagModel[] tags) {
        this.tags = tags;
    }

    public ComponentsModel getComponents() {
        return components;
    }

    public void setComponents(ComponentsModel components) {
        this.components = components;
    }
}
