package com.chocomall.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RbacTest extends AdminTestSupport {

    private long roleId(String token, String name) throws Exception {
        for (JsonNode r : call("GET", "/role/listAll", null, token, 200)) {
            if (r.get("name").asText().equals(name)) {
                return r.get("id").asLong();
            }
        }
        throw new AssertionError("role " + name);
    }

    private String newStaff(String token, String... roles) throws Exception {
        String username = "staff" + UUID.randomUUID().toString().substring(0, 8);
        long id = call("POST", "/admin/register", Map.of("username", username, "password", "Staff-Pass-1"), token, 201).get("id").asLong();
        List<Long> ids = new ArrayList<>();
        for (String r : roles) {
            ids.add(roleId(token, r));
        }
        call("POST", "/admin/role/update", Map.of("adminId", id, "roleIds", ids), token, 200);
        return username;
    }

    @Test
    void signInIssuesATokenAndProfileShowsRolesAndMenus() throws Exception {
        String token = superAdmin();
        JsonNode info = call("GET", "/admin/info", null, token, 200);

        assertThat(info.get("roles").toString()).contains("SUPER_ADMIN");
        assertThat(info.get("menus").size()).isGreaterThanOrEqualTo(8);
    }

    @Test
    void wrongCredentialsAndMissingTokensAreRejected() throws Exception {
        message("POST", "/admin/login", Map.of("username", "admin", "password", "wrong"), null, 401);
        call("GET", "/product/list", null, null, 401);
        call("GET", "/product/list", null, "not-a-token", 401);
    }

    @Test
    void repeatedFailuresLockSignInTemporarily() throws Exception {
        String user = newStaff(superAdmin(), "CONTENT_EDITOR");
        for (int i = 0; i < 3; i++) {
            call("POST", "/admin/login", Map.of("username", user, "password", "bad-password"), null, 401);
        }
        assertThat(message("POST", "/admin/login", Map.of("username", user, "password", "Staff-Pass-1"), null, 429)).contains("Too many failed");
    }

    @Test
    void accessFollowsTheRolesResources() throws Exception {
        String catalog = login("catalog", DEMO_PASSWORD);   // CATALOG_MANAGER
        String orders = login("orders", DEMO_PASSWORD);     // ORDER_MANAGER

        call("GET", "/product/list", null, catalog, 200);
        call("GET", "/stock/list", null, catalog, 200);
        call("GET", "/order/list", null, catalog, 403);
        call("GET", "/role/listAll", null, catalog, 403);
        call("GET", "/admin/list", null, catalog, 403);

        call("GET", "/order/list", null, orders, 200);
        call("GET", "/member/list", null, orders, 200);
        call("GET", "/product/list", null, orders, 403);
        call("GET", "/guide/progress", null, orders, 403);
    }

    @Test
    void theMenuTreeOnlyContainsWhatTheRoleMayOpen() throws Exception {
        JsonNode menus = call("GET", "/admin/info", null, login("catalog", DEMO_PASSWORD), 200).get("menus");
        List<String> titles = new ArrayList<>();
        menus.forEach(m -> titles.add(m.get("title").asText()));

        assertThat(titles).contains("Catalog", "Marketing").doesNotContain("System", "Orders");
    }

    @Test
    void disablingARoleTakesEffectImmediately() throws Exception {
        String admin = superAdmin();
        call("POST", "/role/create", Map.of("name", "TEMP_" + UUID.randomUUID().toString().substring(0, 6), "description", "temp"), admin, 200);
        // reuse the seeded CONTENT_EDITOR for a fresh user to avoid depending on other tests
        String user = newStaff(admin, "CONTENT_EDITOR");
        String token = login(user, "Staff-Pass-1");
        call("GET", "/subject/list", null, token, 200);

        long editor = roleId(admin, "CONTENT_EDITOR");
        call("POST", "/role/updateStatus/" + editor + "?status=0", null, admin, 200);
        call("GET", "/subject/list", null, token, 403);

        call("POST", "/role/updateStatus/" + editor + "?status=1", null, admin, 200);
        call("GET", "/subject/list", null, token, 200);
    }

    @Test
    void aCustomRoleGrantsExactlyTheAllocatedResources() throws Exception {
        String admin = superAdmin();
        String name = "REPORTER_" + UUID.randomUUID().toString().substring(0, 6);
        long role = call("POST", "/role/create", Map.of("name", name, "description", "reports only"), admin, 200).get("id").asLong();
        long reportResource = 0;
        for (JsonNode r : call("GET", "/resource/listAll", null, admin, 200)) {
            if (r.get("url").asText().equals("/report/**")) {
                reportResource = r.get("id").asLong();
            }
        }
        call("POST", "/role/allocResource", Map.of("roleId", role, "ids", Set.of(reportResource)), admin, 200);
        String user = newStaff(admin, name);
        String token = login(user, "Staff-Pass-1");

        call("GET", "/report/dashboard", null, token, 200);
        call("GET", "/product/list", null, token, 403);
    }

    @Test
    void theSuperAdminRoleIsProtected() throws Exception {
        String admin = superAdmin();
        long superRole = roleId(admin, "SUPER_ADMIN");

        message("POST", "/role/delete/" + superRole, null, admin, 400);
        message("POST", "/role/updateStatus/" + superRole + "?status=0", null, admin, 400);
        message("POST", "/role/update/" + superRole, Map.of("name", "RENAMED"), admin, 400);
    }

    @Test
    void theLastSuperAdminCannotBeRemovedDisabledOrDemoted() throws Exception {
        String admin = superAdmin();
        long adminId = 0;
        for (JsonNode a : call("GET", "/admin/list?keyword=admin&pageSize=50", null, admin, 200).get("list")) {
            if (a.get("username").asText().equals("admin")) {
                adminId = a.get("id").asLong();
            }
        }
        // the signed-in account cannot delete or disable itself
        assertThat(message("POST", "/admin/delete/" + adminId, null, admin, 400)).contains("own account");
        assertThat(message("POST", "/admin/updateStatus/" + adminId + "?status=0", null, admin, 400)).contains("own account");
        // and removing the only super-admin role is refused
        assertThat(message("POST", "/admin/role/update", Map.of("adminId", adminId, "roleIds", Set.of()), admin, 409)).contains("At least one super admin");
    }

    @Test
    void changingPasswordsRequiresTheCurrentOneAndInvalidatesTheOldOne() throws Exception {
        String admin = superAdmin();
        String user = newStaff(admin, "CONTENT_EDITOR");
        String token = login(user, "Staff-Pass-1");

        message("POST", "/admin/password", Map.of("username", user, "oldPassword", "wrong", "newPassword", "New-Password-2"), token, 400);
        call("POST", "/admin/password", Map.of("username", user, "oldPassword", "Staff-Pass-1", "newPassword", "New-Password-2"), token, 200);

        call("POST", "/admin/login", Map.of("username", user, "password", "Staff-Pass-1"), null, 401);
        login(user, "New-Password-2");
    }

    @Test
    void aDisabledAccountCannotUseItsExistingToken() throws Exception {
        String admin = superAdmin();
        String user = newStaff(admin, "CONTENT_EDITOR");
        String token = login(user, "Staff-Pass-1");
        long id = 0;
        for (JsonNode a : call("GET", "/admin/list?keyword=" + user, null, admin, 200).get("list")) {
            id = a.get("id").asLong();
        }

        call("POST", "/admin/updateStatus/" + id + "?status=0", null, admin, 200);

        call("GET", "/subject/list", null, token, 401);
    }

    @Test
    void passwordsAreNeverReturnedByTheApi() throws Exception {
        String body = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/admin/list")
                .header("Authorization", "Bearer " + superAdmin())).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("password").doesNotContain("$2a$");
    }
}
