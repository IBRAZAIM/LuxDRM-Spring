package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Category;
import kz.ibrazaim.catalog.model.Option;
import kz.ibrazaim.catalog.repository.OptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class OptionService{
    private final OptionRepository optionRepository;

    public void create(List<String> optionNames, Category category) {
        if (optionNames != null) {
            for (String optionName : optionNames) {
                Option option = new Option();
                option.setName(optionName);
                option.setCategory(category);
                optionRepository.save(option);
            }
        }
    }
}
