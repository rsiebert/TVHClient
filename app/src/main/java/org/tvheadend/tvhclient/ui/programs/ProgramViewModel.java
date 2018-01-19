package org.tvheadend.tvhclient.ui.programs;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import org.tvheadend.tvhclient.data.DataRepository;
import org.tvheadend.tvhclient.data.model.Program;

import java.util.List;

public class ProgramViewModel extends AndroidViewModel {

    private final DataRepository repository;
    private LiveData<List<Program>> programs;

    public ProgramViewModel(Application application) {
        super(application);
        repository = DataRepository.getInstance(application);
        programs = repository.getPrograms();
    }

    public LiveData<List<Program>> getPrograms() {
        return programs;
    }

    public LiveData<Program> getProgram(int id) {
        return repository.getProgram(id);
    }

    public Program getProgramSync(int id) {
        return repository.getProgramFromDatabase(id);
    }
}
