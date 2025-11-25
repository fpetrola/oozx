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
    public List<String> controls;
    public List<String> awards;
    public String remarks;
    public String hardwareBlurb;
    public String knownErrors;
    public List<ReviewAward> reviewAwards;
    public List<Publisher> publishers;
    public List<Author> authors;
    public List<Release> releases;
    public List<String> series;
    public List<String> copyright;
    public List<String> features;
    public List<String> featuresZX81;
    public List<String> competition;
    public List<String> themedGroup;
    public List<UnsortedGroup> unsortedGroup;
    public List<OtherSystem> otherSystems;
    public List<String> inCompilations;
    public List<CompilationContent> compilationContents;
    public List<String> authoredWith;
    public List<String> authoring;
    public List<String> editorOf;
    public List<String> editBy;
    public List<String> requiresHardware;
    public List<String> requiredByHardware;
    public List<String> inspirationFor;
    public List<String> inspiredBy;
    public List<String> addOnDependsOn;
    public List<String> addOnAvailable;
    public List<String> modificationOf;
    public List<String> modifiedBy;
    public List<String> otherPlatform;
    public List<String> runsWith;
    public List<String> requiredToRun;
    public List<String> derivedFrom;
    public List<String> originOf;
    public List<String> bundledWith;
    public List<String> bundleContent;
    public List<String> duplicateOf;
    public List<String> duplicatedBy;
    public List<String> inBook;
    public List<String> bookContents;
    public List<TosecEntry> tosec;
    public List<RelatedLink> relatedLinks;
    public List<RelatedLink> relatedSites;
    public List<String> youTubeLinks;
    public List<AdditionalDownload> additionalDownloads;
    public List<MagazineReference> magazineReferences;
    public List<Screen> screens;
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