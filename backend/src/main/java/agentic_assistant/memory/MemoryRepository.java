
package agentic_assistant.memory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemoryRepository
        extends JpaRepository<MemoryEntry, Long> {

    List<MemoryEntry> findByMemoryKeyContainingIgnoreCase(
            String memoryKey);

    Optional<MemoryEntry> findFirstByMemoryKeyIgnoreCaseOrderByCreatedAtDesc(
            String memoryKey);
}