package com.raulbolivar.usecase.loader;

import com.raulbolivar.model.user.User;
import com.raulbolivar.ports.IJSONPlaceHolderUserGateway;
import com.raulbolivar.usecase.UserUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserMocksStartupLoaderTest {

    @Mock
    private UserUseCase userUseCase;

    @Mock
    private IJSONPlaceHolderUserGateway jsonPlaceHolderUserGateway;

    private UserMocksStartupLoader loader;

    @BeforeEach
    void setUp() {
        loader = new UserMocksStartupLoader(userUseCase, jsonPlaceHolderUserGateway);
    }

    @Test
    void shouldLoadUsersFromJsonPlaceholderAndInsertIntoH2AtStartup() {
        ReflectionTestUtils.setField(loader, "autoLoadScripts", true);

        User valid = new User("", null, "", "Leanne Graham", null, 1, "leanne@demo.com", "leanne");
        User invalidWithoutName = new User("", null, "", "   ", null, 2, "skip@demo.com", "skip");

        when(jsonPlaceHolderUserGateway.getUsers()).thenReturn(Flux.fromIterable(List.of(valid, invalidWithoutName)));
        when(userUseCase.loadSqlScript(anyString(), anyBoolean())).thenReturn(Mono.just(1));

        loader.run(new DefaultApplicationArguments());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(userUseCase).loadSqlScript(sqlCaptor.capture(), org.mockito.ArgumentMatchers.eq(true));

        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("Leanne Graham"));
        assertTrue(sql.contains("source"));
        assertFalse(sql.contains("skip@demo.com"));
    }

    @Test
    void shouldSkipStartupLoadWhenDisabled() {
        ReflectionTestUtils.setField(loader, "autoLoadScripts", false);

        loader.run(new DefaultApplicationArguments());

        verify(jsonPlaceHolderUserGateway, never()).getUsers();
        verify(userUseCase, never()).loadSqlScript(anyString(), anyBoolean());
    }

    @Test
    void shouldNotCallPersistenceWhenJsonPlaceholderReturnsEmptyList() {
        ReflectionTestUtils.setField(loader, "autoLoadScripts", true);

        when(jsonPlaceHolderUserGateway.getUsers()).thenReturn(Flux.empty());

        loader.run(new DefaultApplicationArguments());

        verify(userUseCase, never()).loadSqlScript(anyString(), anyBoolean());
    }
}
