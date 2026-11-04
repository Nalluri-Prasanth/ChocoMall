package com.chocomall.admin.service;

import com.chocomall.admin.dto.SalesDtos.HelpArticleParam;
import com.chocomall.admin.dto.SalesDtos.HelpCategoryParam;
import com.chocomall.admin.dto.SalesDtos.PairingParam;
import com.chocomall.admin.dto.SalesDtos.ServingParam;
import com.chocomall.admin.dto.SalesDtos.SubjectParam;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.PageParams;
import com.chocomall.model.entity.FoodPairing;
import com.chocomall.model.entity.HelpArticle;
import com.chocomall.model.entity.HelpCategory;
import com.chocomall.model.entity.ServingFormat;
import com.chocomall.model.entity.Subject;
import com.chocomall.model.enums.Priority;
import com.chocomall.model.repository.FoodPairingRepository;
import com.chocomall.model.repository.HelpArticleRepository;
import com.chocomall.model.repository.HelpCategoryRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ServingFormatRepository;
import com.chocomall.model.repository.SubjectRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Editorial subjects, the help centre, the food-pairing guide and the serving-format guide. */
@Service
@RequiredArgsConstructor
@Transactional
public class ContentAdminService {

    private final SubjectRepository subjects;
    private final HelpCategoryRepository helpCategories;
    private final HelpArticleRepository helpArticles;
    private final FoodPairingRepository pairings;
    private final ServingFormatRepository servings;
    private final ProductRepository products;

    // ---------------------------------------------------------------------------- subjects

    @Transactional(readOnly = true)
    public CommonPage<Subject> subjects(String keyword, String category, Integer pageNum, Integer pageSize) {
        return CommonPage.of(subjects.search(blank(keyword), blank(category),
                PageParams.of(pageNum, pageSize, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")))));
    }

    @Transactional(readOnly = true)
    public Subject subject(Long id) {
        return Asserts.found(subjects.findById(id), "Subject");
    }

    public Subject createSubject(SubjectParam p) {
        Subject s = new Subject();
        applySubject(s, p);
        return subjects.save(s);
    }

    public Subject updateSubject(Long id, SubjectParam p) {
        Subject s = subject(id);
        applySubject(s, p);
        return s;
    }

    public void deleteSubjects(Set<Long> ids) {
        subjects.deleteAllById(ids);
    }

    private void applySubject(Subject s, SubjectParam p) {
        Set<Long> productIds = p.productIds() == null ? Set.of() : p.productIds();
        Asserts.isTrue(products.findAllById(productIds).size() == productIds.size(), "One of the products does not exist");
        s.setTitle(p.title());
        s.setPic(p.pic());
        s.setDescription(p.description());
        s.setContent(p.content());
        s.setCategory(p.category());
        s.setShowStatus(p.showStatus() == null || p.showStatus());
        s.setRecommendStatus(Boolean.TRUE.equals(p.recommendStatus()));
        s.setSort(p.sort() == null ? 0 : p.sort());
        s.getProductIds().clear();
        s.getProductIds().addAll(productIds);
    }

    // ---------------------------------------------------------------------------- help centre

    @Transactional(readOnly = true)
    public List<HelpCategory> helpCategories() {
        return helpCategories.findAll(Sort.by(Sort.Order.desc("sort")));
    }

    public HelpCategory createHelpCategory(HelpCategoryParam p) {
        HelpCategory c = new HelpCategory();
        applyHelpCategory(c, p);
        return helpCategories.save(c);
    }

    public HelpCategory updateHelpCategory(Long id, HelpCategoryParam p) {
        HelpCategory c = Asserts.found(helpCategories.findById(id), "Help category");
        applyHelpCategory(c, p);
        return c;
    }

    public void deleteHelpCategory(Long id) {
        Asserts.state(helpArticles.findByCategoryIdAndShowStatusTrue(id).isEmpty(), "The category still has articles");
        helpCategories.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<HelpArticle> articles(Long categoryId) {
        return helpArticles.findAll().stream().filter(a -> categoryId == null || categoryId.equals(a.getCategoryId())).toList();
    }

    public HelpArticle createArticle(HelpArticleParam p) {
        HelpArticle a = new HelpArticle();
        applyArticle(a, p);
        return helpArticles.save(a);
    }

    public HelpArticle updateArticle(Long id, HelpArticleParam p) {
        HelpArticle a = Asserts.found(helpArticles.findById(id), "Article");
        applyArticle(a, p);
        return a;
    }

    public void deleteArticle(Long id) {
        helpArticles.deleteById(id);
    }

    private void applyHelpCategory(HelpCategory c, HelpCategoryParam p) {
        c.setName(p.name());
        c.setSort(p.sort() == null ? 0 : p.sort());
        c.setShowStatus(p.showStatus() == null || p.showStatus());
    }

    private void applyArticle(HelpArticle a, HelpArticleParam p) {
        Asserts.found(helpCategories.findById(p.categoryId()), "Help category");
        a.setCategoryId(p.categoryId());
        a.setTitle(p.title());
        a.setContent(p.content());
        a.setShowStatus(p.showStatus() == null || p.showStatus());
    }

    // ---------------------------------------------------------------------------- pairings & serving formats

    @Transactional(readOnly = true)
    public CommonPage<FoodPairing> pairings(String group, String keyword, Priority priority, Integer pageNum, Integer pageSize) {
        return CommonPage.of(pairings.search(blank(group), blank(keyword), priority,
                PageParams.of(pageNum, pageSize, Sort.by("code"))));
    }

    @Transactional(readOnly = true)
    public List<String> pairingGroups() {
        return pairings.groups();
    }

    public FoodPairing createPairing(PairingParam p) {
        Asserts.state(pairings.findAll().stream().noneMatch(x -> x.getCode().equals(p.code())), "Pairing code " + p.code() + " already exists");
        FoodPairing f = new FoodPairing();
        applyPairing(f, p);
        return pairings.save(f);
    }

    public FoodPairing updatePairing(Long id, PairingParam p) {
        FoodPairing f = Asserts.found(pairings.findById(id), "Pairing");
        applyPairing(f, p);
        return f;
    }

    public void deletePairing(Long id) {
        pairings.deleteById(id);
    }

    private void applyPairing(FoodPairing f, PairingParam p) {
        f.setCode(p.code());
        f.setFoodGroup(p.foodGroup());
        f.setFood(p.food());
        f.setChocolateCombination(p.chocolateCombination());
        f.setShopFormat(p.shopFormat());
        f.setPriority(p.priority());
    }

    @Transactional(readOnly = true)
    public CommonPage<ServingFormat> servings(String keyword, Priority priority, Integer pageNum, Integer pageSize) {
        return CommonPage.of(servings.search(blank(keyword), priority, PageParams.of(pageNum, pageSize, Sort.by("code"))));
    }

    public ServingFormat createServing(ServingParam p) {
        Asserts.state(servings.findAll().stream().noneMatch(x -> x.getCode().equals(p.code())), "Serving code " + p.code() + " already exists");
        ServingFormat s = new ServingFormat();
        applyServing(s, p);
        return servings.save(s);
    }

    public ServingFormat updateServing(Long id, ServingParam p) {
        ServingFormat s = Asserts.found(servings.findById(id), "Serving format");
        applyServing(s, p);
        return s;
    }

    public void deleteServing(Long id) {
        servings.deleteById(id);
    }

    private void applyServing(ServingFormat s, ServingParam p) {
        s.setCode(p.code());
        s.setEatingFormat(p.eatingFormat());
        s.setHowToServe(p.howToServe());
        s.setSuitableProduct(p.suitableProduct());
        s.setSalesApplication(p.salesApplication());
        s.setPriority(p.priority());
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
