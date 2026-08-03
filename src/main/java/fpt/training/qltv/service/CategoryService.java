package fpt.training.qltv.service;

import fpt.training.qltv.dto.request.CreateCategoryRequest;
import fpt.training.qltv.dto.request.UpdateCategoryRequest;
import fpt.training.qltv.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {

    List<CategoryResponse> findAll();

    List<CategoryResponse> findAllDeleted();

    CategoryResponse findById(Long id);

    CategoryResponse create(CreateCategoryRequest request);

    CategoryResponse update(Long id, UpdateCategoryRequest request);

    void delete(Long id);

    void restore(Long id);

    void forceDelete(Long id);
}
