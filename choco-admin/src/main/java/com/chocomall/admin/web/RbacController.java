package com.chocomall.admin.web;

import com.chocomall.admin.dto.AuthDtos.AllocParam;
import com.chocomall.admin.dto.AuthDtos.CategoryParam;
import com.chocomall.admin.dto.AuthDtos.MenuNode;
import com.chocomall.admin.dto.AuthDtos.MenuParam;
import com.chocomall.admin.dto.AuthDtos.ResourceParam;
import com.chocomall.admin.dto.AuthDtos.RoleParam;
import com.chocomall.admin.service.RbacService;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.api.CommonResult;
import com.chocomall.model.entity.ApiResource;
import com.chocomall.model.entity.Menu;
import com.chocomall.model.entity.ResourceCategory;
import com.chocomall.model.entity.Role;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Roles, menus, resources and resource categories. */
@Tag(name = "Permissions")
@RestController
@RequiredArgsConstructor
public class RbacController {

    private final RbacService rbac;

    // ---- roles
    @PostMapping("/role/create")
    public CommonResult<Role> createRole(@Valid @RequestBody RoleParam p) {
        return CommonResult.success(rbac.createRole(p));
    }

    @PostMapping("/role/update/{id}")
    public CommonResult<Role> updateRole(@PathVariable Long id, @Valid @RequestBody RoleParam p) {
        return CommonResult.success(rbac.updateRole(id, p));
    }

    @PostMapping("/role/delete/{id}")
    public CommonResult<Void> deleteRole(@PathVariable Long id) {
        rbac.deleteRole(id);
        return CommonResult.success();
    }

    @PostMapping("/role/updateStatus/{id}")
    public CommonResult<Void> roleStatus(@PathVariable Long id, @RequestParam Integer status) {
        rbac.setRoleStatus(id, status);
        return CommonResult.success();
    }

    @GetMapping("/role/list")
    public CommonResult<CommonPage<Role>> roles(@RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(rbac.listRoles(keyword, pageNum, pageSize));
    }

    @GetMapping("/role/listAll")
    public CommonResult<List<Role>> allRoles() {
        return CommonResult.success(rbac.allRoles());
    }

    @GetMapping("/role/listMenu/{roleId}")
    public CommonResult<Set<Long>> roleMenus(@PathVariable Long roleId) {
        return CommonResult.success(rbac.roleMenuIds(roleId));
    }

    @GetMapping("/role/listResource/{roleId}")
    public CommonResult<Set<Long>> roleResources(@PathVariable Long roleId) {
        return CommonResult.success(rbac.roleResourceIds(roleId));
    }

    @PostMapping("/role/allocMenu")
    public CommonResult<Void> allocMenus(@Valid @RequestBody AllocParam p) {
        rbac.allocMenus(p.roleId(), p.ids());
        return CommonResult.success();
    }

    @PostMapping("/role/allocResource")
    public CommonResult<Void> allocResources(@Valid @RequestBody AllocParam p) {
        rbac.allocResources(p.roleId(), p.ids());
        return CommonResult.success();
    }

    // ---- menus
    @PostMapping("/menu/create")
    public CommonResult<Menu> createMenu(@Valid @RequestBody MenuParam p) {
        return CommonResult.success(rbac.createMenu(p));
    }

    @PostMapping("/menu/update/{id}")
    public CommonResult<Menu> updateMenu(@PathVariable Long id, @Valid @RequestBody MenuParam p) {
        return CommonResult.success(rbac.updateMenu(id, p));
    }

    @PostMapping("/menu/delete/{id}")
    public CommonResult<Void> deleteMenu(@PathVariable Long id) {
        rbac.deleteMenu(id);
        return CommonResult.success();
    }

    @GetMapping("/menu/{id}")
    public CommonResult<Menu> menu(@PathVariable Long id) {
        return CommonResult.success(rbac.getMenu(id));
    }

    @GetMapping("/menu/list/{parentId}")
    public CommonResult<CommonPage<Menu>> menus(@PathVariable Long parentId,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(rbac.listMenus(parentId, pageNum, pageSize));
    }

    @GetMapping("/menu/treeList")
    public CommonResult<List<MenuNode>> menuTree() {
        return CommonResult.success(rbac.menuTree());
    }

    // ---- resources
    @PostMapping("/resource/create")
    public CommonResult<ApiResource> createResource(@Valid @RequestBody ResourceParam p) {
        return CommonResult.success(rbac.createResource(p));
    }

    @PostMapping("/resource/update/{id}")
    public CommonResult<ApiResource> updateResource(@PathVariable Long id, @Valid @RequestBody ResourceParam p) {
        return CommonResult.success(rbac.updateResource(id, p));
    }

    @PostMapping("/resource/delete/{id}")
    public CommonResult<Void> deleteResource(@PathVariable Long id) {
        rbac.deleteResource(id);
        return CommonResult.success();
    }

    @GetMapping("/resource/{id}")
    public CommonResult<ApiResource> resource(@PathVariable Long id) {
        return CommonResult.success(rbac.getResource(id));
    }

    @GetMapping("/resource/list")
    public CommonResult<CommonPage<ApiResource>> resources(@RequestParam(required = false) Long categoryId,
                                                           @RequestParam(required = false) String nameKeyword,
                                                           @RequestParam(required = false) String urlKeyword,
                                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(rbac.searchResources(categoryId, nameKeyword, urlKeyword, pageNum, pageSize));
    }

    @GetMapping("/resource/listAll")
    public CommonResult<List<ApiResource>> allResources() {
        return CommonResult.success(rbac.allResources());
    }

    // ---- resource categories
    @PostMapping("/resourceCategory/create")
    public CommonResult<ResourceCategory> createCategory(@Valid @RequestBody CategoryParam p) {
        return CommonResult.success(rbac.createCategory(p));
    }

    @PostMapping("/resourceCategory/update/{id}")
    public CommonResult<ResourceCategory> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryParam p) {
        return CommonResult.success(rbac.updateCategory(id, p));
    }

    @PostMapping("/resourceCategory/delete/{id}")
    public CommonResult<Void> deleteCategory(@PathVariable Long id) {
        rbac.deleteCategory(id);
        return CommonResult.success();
    }

    @GetMapping("/resourceCategory/listAll")
    public CommonResult<List<ResourceCategory>> allCategories() {
        return CommonResult.success(rbac.allCategories());
    }
}
