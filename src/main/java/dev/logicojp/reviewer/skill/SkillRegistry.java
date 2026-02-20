package dev.logicojp.reviewer.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/// Registry for managing skill definitions.
/// Skills can be registered and retrieved by ID.
@Singleton
public class SkillRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SkillRegistry.class);

    private final Map<String, SkillDefinition> skills = new ConcurrentHashMap<>();

    public void register(SkillDefinition skill) {
        skills.put(skill.id(), skill);
        logger.info("Registered skill: {} ({})", skill.id(), skill.name());
    }

    public void registerAll(Collection<SkillDefinition> skillDefinitions) {
        skillDefinitions.forEach(this::register);
    }

    public Optional<SkillDefinition> get(String skillId) {
        return Optional.ofNullable(skills.get(skillId));
    }

    public List<SkillDefinition> getAll() {
        return List.copyOf(skills.values());
    }

    Set<String> getSkillIds() {
        return Set.copyOf(skills.keySet());
    }

    boolean hasSkill(String skillId) {
        return skills.containsKey(skillId);
    }

    void unregister(String skillId) {
        skills.remove(skillId);
        logger.info("Unregistered skill: {}", skillId);
    }

    void clear() {
        skills.clear();
        logger.info("Cleared all skills");
    }

    public int size() {
        return skills.size();
    }
}
