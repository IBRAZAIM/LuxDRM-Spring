package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Category;
import kz.ibrazaim.catalog.model.Option;
import kz.ibrazaim.catalog.repository.OptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OptionService {
    private final OptionRepository optionRepository;

    @Transactional
    public void create(List<String> optionNames, Category category) {
        if (optionNames == null || optionNames.isEmpty()) {
            return;
        }
        for (String optionName : optionNames) {
            if (optionName != null && !optionName.trim().isEmpty()) {
                Option option = new Option();
                option.setName(optionName.trim());
                option.setCategory(category);
                optionRepository.save(option);
            }
        }
    }

    public List<Option> findAllByCategoryId(long categoryId) {
        return optionRepository.findAllByCategoryId(categoryId);
    }

    @Transactional
    public void deleteAllByCategoryId(long categoryId) {
        List<Option> options = optionRepository.findAllByCategoryId(categoryId);
        if (!options.isEmpty()) {
            optionRepository.deleteAll(options);
        }
    }
}
