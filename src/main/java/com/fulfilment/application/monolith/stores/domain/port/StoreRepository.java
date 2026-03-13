
package com.fulfilment.application.monolith.stores.domain.port;

import com.fulfilment.application.monolith.stores.domain.model.Store;
import java.util.List;

public interface StoreRepository {
    Store save(Store store);
    Store findById(Long id);
    List<Store> findAll();
    void delete(Long id);
}
