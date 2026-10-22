package api.dao.comparison;

import api.dao.BaseDao;
import api.models.BaseModel;
import org.assertj.core.api.AbstractAssert;

public class DaoAndModelAssertions {
    private static final DaoComparator daoComparator = new DaoComparator();

    public static DaoModelAssert assertThat(BaseModel apiModel, BaseDao daoModel) {
        return new DaoModelAssert(apiModel, daoModel);
    }

    public static class DaoModelAssert extends AbstractAssert<DaoModelAssert, Object> {
        private final BaseModel dtoModel;
        private final BaseDao daoModel;

        public DaoModelAssert(BaseModel dtoModel, BaseDao daoModel) {
            super(dtoModel, DaoModelAssert.class);
            this.dtoModel = dtoModel;
            this.daoModel = daoModel;
        }

        public DaoModelAssert match() {
            if (dtoModel == null) {
                failWithMessage("API model should not be null");
            }

            if (daoModel == null) {
                failWithMessage("DAO model should not be null");
            }

            // Use configurable comparison
            try {
                daoComparator.compare(dtoModel, daoModel);
            } catch (AssertionError e) {
                failWithMessage(e.getMessage());
            }

            return this;
        }
    }
}
