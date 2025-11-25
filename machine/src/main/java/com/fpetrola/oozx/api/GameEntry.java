/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

// src/main/java/com/example/GameEntry.java
package com.fpetrola.oozx.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GameEntry {
    // Metadata from Elasticsearch
    public String _index;
    public String _id;
    public Integer _version;
    public Integer _seq_no;
    public Integer _primary_term;
    public Boolean found;
    
    // Helper for main usage
    public String id;
    
    // Basic info
    public String contentType;
    public String zxinfoVersion;
    public String title;
    public String alsoKnownAs;
    public Integer originalYearOfRelease;
    public Integer originalMonthOfRelease;
    public Integer originalDayOfRelease;
    public String machineType;
    public Integer numberOfPlayers;
    public String multiplayerMode;
    public String multiplayerType;
    public Integer xrated;
    public String genre;
    public String genreType;
    public String genreSubType;
    public String isbn;
    public String language;
    public String originalPublication;
    public String availability;
    
    // Pricing
    public Price originalPrice;
    
    // Score and voting
    public Score score;
    
    // Collections
    public List<Object> controls;
    public List<Object> awards;
    public String remarks;
    public String hardwareBlurb;
    public String knownErrors;
    public List<ReviewAward> reviewAwards;
    public List<Publisher> publishers;
    public List<Author> authors;
    public List<Release> releases;
    public List<Object> series;
    public List<Object> copyright;
    public List<Object> features;
    public List<Object> featuresZX81;
    public List<Object> competition;
    public List<Object> themedGroup;
    public List<UnsortedGroup> unsortedGroup;
    public List<OtherSystem> otherSystems;
    public List<Object> inCompilations;
    public List<CompilationContent> compilationContents;
    public List<Object> authoredWith;
    public List<Object> authoring;
    public List<Object> editorOf;
    public List<Object> editBy;
    public List<Object> requiresHardware;
    public List<Object> requiredByHardware;
    public List<Object> inspirationFor;
    public List<Object> inspiredBy;
    public List<Object> addOnDependsOn;
    public List<Object> addOnAvailable;
    public List<Object> modificationOf;
    public List<Object> modifiedBy;
    public List<Object> otherPlatform;
    public List<Object> runsWith;
    public List<Object> requiredToRun;
    public List<Object> derivedFrom;
    public List<Object> originOf;
    public List<Object> bundledWith;
    public List<Object> bundleContent;
    public List<Object> duplicateOf;
    public List<Object> duplicatedBy;
    public List<Object> inBook;
    public List<Object> bookContents;
    public List<TosecEntry> tosec;
    public List<RelatedLink> relatedLinks;
    public List<RelatedLink> relatedSites;
    public List<Object> youTubeLinks;
    public List<AdditionalDownload> additionalDownloads;
    public List<MagazineReference> magazineReferences;
    public List<Object> screens;
    public List<MD5Hash> md5hash;

    @Override
    public String toString() {
        return title + " (" + originalYearOfRelease + ") - " + 
               (publishers != null && !publishers.isEmpty() ? publishers.get(0).name : "Unknown") +
               " | ID: " + (_id != null ? _id : id);
    }
    
    // Helper classes
    public static class Price {
        public String amount;
        public String currency;
        public Integer prefix;
    }
    
    public static class ReviewAward {
        public String awardName;
        public String magazineName;
        public Integer page;
        public Integer issueId;
        public Integer dateYear;
        public Integer dateMonth;
        public Integer volume;
        public Integer number;
    }
    
    public static class UnsortedGroup {
        public String name;
    }
    
    public static class OtherSystem {
        public String name;
        public String url;
    }
    
    public static class CompilationContent {
        public Integer entry_id;
        public String title;
        public List<Publisher> publishers;
        public String machineType;
        public Integer sequence;
        public String side;
        public String variation;
    }
    
    public static class TosecEntry {
        public String path;
    }
    
    public static class RelatedLink {
        public String siteName;
        public String url;
    }
    
    public static class MagazineReference {
        public String type;
        public String featureName;
        public String magazineName;
        public Integer page;
        public Integer issueId;
        public Integer dateYear;
        public Integer dateMonth;
        public Integer volume;
        public Integer number;
        public String score;
    }
    
    public static class MD5Hash {
        public String archive;
        public String filename;
        public String md5;
        public String sha512;
        public String source;
        public String contenttype;
    }
}