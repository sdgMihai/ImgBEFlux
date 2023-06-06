package com.img.resource.service;

import com.img.resource.filter.Filter;
import com.img.resource.filter.FilterFactory;
import com.img.resource.filter.Filters;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class FilterService {

    public static boolean filterNameEquals(String filter1, String filter2) {
        return filter1.toLowerCase(Locale.ROOT)
                .equals(filter2.toLowerCase(Locale.ROOT));
    }

    /**
     Transform filter names and parameters to list of filters.
     */
    public static List<Filter> getFilters(String filterName
            , String filterParams) {

        List<Filter> filters = new ArrayList<>(1);
        if (filterNameEquals(filterName, Filters.BRIGHTNESS.toString())) {
                double param = 0;
                param = Double.parseDouble(filterParams);

                log.debug(String.format("using level %f", param));

                filters.add(FilterFactory.filterCreate(filterName
                        , (float) param
                        , null
                        , 0
                        , 0
                ));
            } else if (filterNameEquals(filterName, Filters.CONTRAST.toString())) {
                double param = 0;
                log.debug(String.valueOf(param));

                filters.add(FilterFactory.filterCreate(filterName
                        , (float) param
                        , null
                        , 0
                        ,0
                        ));
            } else {
                filters.add(FilterFactory.filterCreate(filterName));
            }

        return filters;
    }
}
