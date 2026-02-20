package dev.logicojp.reviewer.skill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SkillRegistry")
class SkillRegistryTest {

    private SkillRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SkillRegistry();
    }

    @Nested
    @DisplayName("register / get")
    class RegisterGet {

        @Test
        @DisplayName("スキルを登録して取得できる")
        void registerAndGet() {
            var skill = SkillDefinition.of("test-skill", "Test", "desc", "prompt");
            registry.register(skill);

            assertThat(registry.get("test-skill")).isPresent()
                .hasValueSatisfying(s -> assertThat(s.name()).isEqualTo("Test"));
        }

        @Test
        @DisplayName("存在しないIDの場合はemptyを返す")
        void emptyForUnknown() {
            assertThat(registry.get("unknown")).isEmpty();
        }
    }

    @Nested
    @DisplayName("registerAll")
    class RegisterAll {

        @Test
        @DisplayName("複数スキルを一括登録できる")
        void registersBatch() {
            var skill1 = SkillDefinition.of("s1", "Skill 1", "d1", "p1");
            var skill2 = SkillDefinition.of("s2", "Skill 2", "d2", "p2");

            registry.registerAll(List.of(skill1, skill2));

            assertThat(registry.size()).isEqualTo(2);
            assertThat(registry.get("s1")).isPresent();
            assertThat(registry.get("s2")).isPresent();
        }
    }

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("登録済みの全スキルを返す")
        void returnsAll() {
            registry.register(SkillDefinition.of("a", "A", "", "p1"));
            registry.register(SkillDefinition.of("b", "B", "", "p2"));

            assertThat(registry.getAll()).hasSize(2);
        }

        @Test
        @DisplayName("空の場合は空リストを返す")
        void emptyWhenNone() {
            assertThat(registry.getAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("size")
    class Size {

        @Test
        @DisplayName("登録数を返す")
        void returnsCount() {
            assertThat(registry.size()).isZero();
            registry.register(SkillDefinition.of("s1", "S1", "", "p"));
            assertThat(registry.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("パッケージプライベートメソッド")
    class PackagePrivateMethods {

        @Test
        @DisplayName("getSkillIdsが全IDを返す")
        void getSkillIds() {
            registry.register(SkillDefinition.of("alpha", "A", "", "p"));
            registry.register(SkillDefinition.of("beta", "B", "", "p"));

            assertThat(registry.getSkillIds()).containsExactlyInAnyOrder("alpha", "beta");
        }

        @Test
        @DisplayName("hasSkillが存在確認できる")
        void hasSkill() {
            registry.register(SkillDefinition.of("exists", "E", "", "p"));

            assertThat(registry.hasSkill("exists")).isTrue();
            assertThat(registry.hasSkill("nope")).isFalse();
        }

        @Test
        @DisplayName("unregisterでスキルを削除できる")
        void unregister() {
            registry.register(SkillDefinition.of("remove-me", "R", "", "p"));
            assertThat(registry.hasSkill("remove-me")).isTrue();

            registry.unregister("remove-me");
            assertThat(registry.hasSkill("remove-me")).isFalse();
        }

        @Test
        @DisplayName("clearで全スキルを削除できる")
        void clear() {
            registry.register(SkillDefinition.of("s1", "S1", "", "p"));
            registry.register(SkillDefinition.of("s2", "S2", "", "p"));

            registry.clear();
            assertThat(registry.size()).isZero();
        }
    }
}
