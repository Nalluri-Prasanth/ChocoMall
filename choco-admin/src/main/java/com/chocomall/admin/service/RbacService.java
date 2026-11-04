package com.chocomall.admin.service;

import com.chocomall.admin.dto.AuthDtos.CategoryParam;
import com.chocomall.admin.dto.AuthDtos.MenuNode;
import com.chocomall.admin.dto.AuthDtos.MenuParam;
import com.chocomall.admin.dto.AuthDtos.ResourceParam;
import com.chocomall.admin.dto.AuthDtos.RoleParam;
import com.chocomall.admin.security.AdminCacheEvictor;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.PageParams;
import com.chocomall.model.entity.ApiResource;
import com.chocomall.model.entity.Menu;
import com.chocomall.model.entity.ResourceCategory;
import com.chocomall.model.entity.Role;
import com.chocomall.model.repository.AdminUserRepository;
import com.chocomall.model.repository.ApiResourceRepository;
import com.chocomall.model.repository.MenuRepository;
import com.chocomall.model.repository.ResourceCategoryRepository;
import com.chocomall.model.repository.RoleRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Roles, menus, API resources and resource categories (the permission model of the back office). */
@Service
@RequiredArgsConstructor
@Transactional
public class RbacService {

    private final RoleRepository roles;
    private final MenuRepository menus;
    private final ApiResourceRepository resources;
    private final ResourceCategoryRepository categories;
    private final AdminUserRepository admins;
    private final AdminCacheEvictor cacheEvictor;

    // ---------------------------------------------------------------------------- roles

    public Role createRole(RoleParam p) {
        Asserts.state(roles.findByName(p.name()).isEmpty(), "A role with that name already exists");
        Role r = new Role();
        r.setName(p.name());
        r.setDescription(p.description());
        r.setSort(p.sort() == null ? 0 : p.sort());
        return roles.save(r);
    }

    public Role updateRole(Long id, RoleParam p) {
        Role r = role(id);
        Asserts.isTrue(!AdminAccountService.SUPER_ADMIN.equals(r.getName()), "The SUPER_ADMIN role cannot be edited");
        r.setName(p.name());
        r.setDescription(p.description());
        if (p.sort() != null) {
            r.setSort(p.sort());
        }
        return r;
    }

    public void deleteRole(Long id) {
        Role r = role(id);
        Asserts.isTrue(!AdminAccountService.SUPER_ADMIN.equals(r.getName()), "The SUPER_ADMIN role cannot be deleted");
        Asserts.state(admins.countByRole(id) == 0, "The role is still assigned to staff accounts");
        roles.delete(r);
        cacheEvictor.evictAll();
    }

    public void setRoleStatus(Long id, int status) {
        Role r = role(id);
        Asserts.isTrue(!AdminAccountService.SUPER_ADMIN.equals(r.getName()), "The SUPER_ADMIN role cannot be disabled");
        r.setStatus(status);
        cacheEvictor.evictAll();
    }

    @Transactional(readOnly = true)
    public CommonPage<Role> listRoles(String keyword, Integer pageNum, Integer pageSize) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return CommonPage.of(roles.search(kw, PageParams.of(pageNum, pageSize)));
    }

    @Transactional(readOnly = true)
    public List<Role> allRoles() {
        return roles.findAll();
    }

    @Transactional(readOnly = true)
    public Set<Long> roleMenuIds(Long roleId) {
        return role(roleId).getMenuIds();
    }

    @Transactional(readOnly = true)
    public Set<Long> roleResourceIds(Long roleId) {
        return role(roleId).getResourceIds();
    }

    public void allocMenus(Long roleId, Set<Long> menuIds) {
        Role r = editableRole(roleId);
        Asserts.isTrue(menus.findAllById(menuIds).size() == menuIds.size(), "One of the menus does not exist");
        r.getMenuIds().clear();
        r.getMenuIds().addAll(menuIds);
        cacheEvictor.evictAll();
    }

    public void allocResources(Long roleId, Set<Long> resourceIds) {
        Role r = editableRole(roleId);
        Asserts.isTrue(resources.findAllById(resourceIds).size() == resourceIds.size(), "One of the resources does not exist");
        r.getResourceIds().clear();
        r.getResourceIds().addAll(resourceIds);
        cacheEvictor.evictAll();
    }

    private Role editableRole(Long id) {
        Role r = role(id);
        Asserts.isTrue(!AdminAccountService.SUPER_ADMIN.equals(r.getName()),
                "SUPER_ADMIN always has every menu and resource");
        return r;
    }

    private Role role(Long id) {
        return Asserts.found(roles.findById(id), "Role");
    }

    // ---------------------------------------------------------------------------- menus

    public Menu createMenu(MenuParam p) {
        Menu m = new Menu();
        applyMenu(m, p);
        return menus.save(m);
    }

    public Menu updateMenu(Long id, MenuParam p) {
        Menu m = Asserts.found(menus.findById(id), "Menu");
        Asserts.isTrue(!id.equals(p.parentId()), "A menu cannot be its own parent");
        applyMenu(m, p);
        return m;
    }

    public void deleteMenu(Long id) {
        Asserts.state(menus.findByParentIdOrderBySortDesc(id).isEmpty(), "Delete the sub-menus first");
        roles.findAll().forEach(r -> r.getMenuIds().remove(id));
        menus.deleteById(id);
        cacheEvictor.evictAll();
    }

    @Transactional(readOnly = true)
    public List<MenuNode> menuTree() {
        return AdminAccountService.menuTree(menus.findAllByOrderBySortDesc());
    }

    @Transactional(readOnly = true)
    public CommonPage<Menu> listMenus(Long parentId, Integer pageNum, Integer pageSize) {
        return CommonPage.of(menus.findByParentId(parentId, PageParams.of(pageNum, pageSize)));
    }

    @Transactional(readOnly = true)
    public Menu getMenu(Long id) {
        return Asserts.found(menus.findById(id), "Menu");
    }

    private void applyMenu(Menu m, MenuParam p) {
        long parent = p.parentId() == null ? 0L : p.parentId();
        m.setParentId(parent);
        m.setLevel(parent == 0 ? 0 : Asserts.found(menus.findById(parent), "Parent menu").getLevel() + 1);
        m.setTitle(p.title());
        m.setName(p.name());
        m.setIcon(p.icon());
        m.setSort(p.sort() == null ? 0 : p.sort());
        m.setHidden(Boolean.TRUE.equals(p.hidden()));
    }

    // ---------------------------------------------------------------------------- resources

    public ApiResource createResource(ResourceParam p) {
        ApiResource r = new ApiResource();
        applyResource(r, p);
        ApiResource saved = resources.save(r);
        // new endpoints are immediately available to super admins
        roles.findByName(AdminAccountService.SUPER_ADMIN).ifPresent(sa -> sa.getResourceIds().add(saved.getId()));
        cacheEvictor.evictAll();
        return saved;
    }

    public ApiResource updateResource(Long id, ResourceParam p) {
        ApiResource r = Asserts.found(resources.findById(id), "Resource");
        applyResource(r, p);
        cacheEvictor.evictAll();
        return r;
    }

    public void deleteResource(Long id) {
        roles.findAll().forEach(role -> role.getResourceIds().remove(id));
        resources.deleteById(id);
        cacheEvictor.evictAll();
    }

    @Transactional(readOnly = true)
    public ApiResource getResource(Long id) {
        return Asserts.found(resources.findById(id), "Resource");
    }

    @Transactional(readOnly = true)
    public CommonPage<ApiResource> searchResources(Long categoryId, String name, String url, Integer pageNum, Integer pageSize) {
        return CommonPage.of(resources.search(categoryId, blank(name), blank(url), PageParams.of(pageNum, pageSize)));
    }

    @Transactional(readOnly = true)
    public List<ApiResource> allResources() {
        return resources.findAll();
    }

    private void applyResource(ApiResource r, ResourceParam p) {
        Asserts.isTrue(p.url().startsWith("/"), "A resource URL pattern must start with /");
        r.setCategoryId(p.categoryId());
        r.setName(p.name());
        r.setUrl(p.url());
        r.setDescription(p.description());
    }

    // ---------------------------------------------------------------------------- resource categories

    public ResourceCategory createCategory(CategoryParam p) {
        ResourceCategory c = new ResourceCategory();
        c.setName(p.name());
        c.setSort(p.sort() == null ? 0 : p.sort());
        return categories.save(c);
    }

    public ResourceCategory updateCategory(Long id, CategoryParam p) {
        ResourceCategory c = Asserts.found(categories.findById(id), "Resource category");
        c.setName(p.name());
        if (p.sort() != null) {
            c.setSort(p.sort());
        }
        return c;
    }

    public void deleteCategory(Long id) {
        Asserts.state(resources.findByCategoryId(id).isEmpty(), "The category still contains resources");
        categories.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ResourceCategory> allCategories() {
        return categories.findAllByOrderBySortDesc();
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
